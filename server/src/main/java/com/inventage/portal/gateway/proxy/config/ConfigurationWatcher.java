package com.inventage.portal.gateway.proxy.config;

import com.inventage.portal.gateway.Runtime;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.listener.Listener;
import com.inventage.portal.gateway.proxy.provider.Provider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It listens to incoming dynamic configurations. Upon passing several checks is
 * passed to all
 * registered listeners. A namespace per provider exists to avoid clashes.
 */
public class ConfigurationWatcher extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationWatcher.class);

    private static final String CONFIG_THROTTLED_ADDRESS = "configuration-watcher-config-throttled";

    private final Vertx vertx;

    private final EventBus eventBus;
    private final Provider provider;
    private final String configurationAddress;
    private final Map<String, JsonObject> currentConfigurations;
    private final int providersThrottleIntervalMs;
    private final List<String> defaultEntrypoints;
    private final Set<String> providerConfigReloadThrottler;
    private final AtomicBoolean anyConfigurationPublished;

    private long timerId;
    private List<Listener> configurationListeners;

    public ConfigurationWatcher(
        Vertx vertx, Provider provider, String configurationAddress,
        int providersThrottleIntervalMs, List<String> defaultEntrypoints
    ) {
        this.vertx = vertx;
        this.eventBus = vertx.eventBus();
        this.provider = provider;
        this.configurationAddress = configurationAddress;
        this.providersThrottleIntervalMs = providersThrottleIntervalMs;
        this.defaultEntrypoints = new ArrayList<String>(defaultEntrypoints);
        this.currentConfigurations = new HashMap<>();
        this.providerConfigReloadThrottler = new HashSet<>();
        this.anyConfigurationPublished = new AtomicBoolean();
    }

    /**
     * If a router has no entrypoint applied, the default entrypoints are set.
     * 
     * @param config
     *            to inspect
     * @param entrypoints
     *            default entrypoints
     * @return
     */
    private static JsonObject applyEntrypoints(JsonObject config, List<String> entrypoints) {
        final JsonObject httpConfig = config.getJsonObject(DynamicConfiguration.HTTP);

        if (httpConfig == null) {
            return config;
        }

        final JsonArray rs = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        for (int i = 0; i < rs.size(); i++) {
            final JsonObject r = rs.getJsonObject(i);
            final JsonArray rEntrypoints = r.getJsonArray(DynamicConfiguration.ROUTER_ENTRYPOINTS);
            final String routerName = r.getString(DynamicConfiguration.ROUTER_NAME);
            if (rEntrypoints == null || rEntrypoints.size() == 0) {
                LOGGER.debug(
                    "No entryPoint defined for the router '{}', using the default one(s) instead '{}'",
                    routerName,
                    entrypoints.toString());
                r.put(DynamicConfiguration.ROUTER_ENTRYPOINTS, new JsonArray(entrypoints));
            }
        }

        return config;
    }

    /**
     * Each provider supplies one configuration. Here, all configurations are merged into one aggregated configuration.
     * 
     * @param configurations
     *            contains the configuration supplied by each provider
     * @return merged configuration of all providers
     */
    private static JsonObject mergeConfigurations(Map<String, JsonObject> configurations) {
        LOGGER.debug("Merge configurations");
        final JsonObject mergedConfig = DynamicConfiguration.buildDefaultConfiguration();
        final JsonObject mergedHttpConfig = mergedConfig.getJsonObject(DynamicConfiguration.HTTP);

        if (mergedHttpConfig == null) {
            return mergedConfig;
        }

        for (Entry<String, JsonObject> provider : configurations.entrySet()) {
            final String providerName = provider.getKey();
            final JsonObject conf = configurations.get(providerName);
            final JsonObject httpConf = conf.getJsonObject(DynamicConfiguration.HTTP);

            if (httpConf != null) {
                final JsonArray rts = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
                final JsonArray mergedRts = mergedHttpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
                mergeRouters(providerName, rts, mergedRts);

                final JsonArray mws = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                final JsonArray mergedMws = mergedHttpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                mergeMiddlewares(providerName, mws, mergedMws);

                final JsonArray svs = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
                final JsonArray mergedSvs = mergedHttpConfig.getJsonArray(DynamicConfiguration.SERVICES);
                mergeServices(providerName, svs, mergedSvs);
            }
        }

        return mergedConfig;
    }

    private static JsonArray mergeRouters(String providerName, JsonArray rts, JsonArray mergedRts) {
        if (rts == null) {
            return mergedRts;
        }

        for (int i = 0; i < rts.size(); i++) {
            final JsonObject rt = rts.getJsonObject(i);

            final String rtName = rt.getString(DynamicConfiguration.ROUTER_NAME);
            rt.put(DynamicConfiguration.ROUTER_NAME, getQualifiedName(providerName, rtName));

            // Service and middlewares may referencing another provider namespace
            // The names are only patched if this is not the case.
            final String svName = rt.getString(DynamicConfiguration.ROUTER_SERVICE);
            rt.put(DynamicConfiguration.ROUTER_SERVICE, getQualifiedName(providerName, svName));

            final JsonArray mwNames = rt.getJsonArray(DynamicConfiguration.ROUTER_MIDDLEWARES);
            if (mwNames != null) {
                final JsonArray qualifiedMwNames = new JsonArray();
                for (int j = 0; j < mwNames.size(); j++) {
                    final String mwName = mwNames.getString(j);
                    qualifiedMwNames.add(getQualifiedName(providerName, mwName));
                }
                rt.put(DynamicConfiguration.ROUTER_MIDDLEWARES, qualifiedMwNames);
            }

            mergedRts.add(rt);
        }
        return mergedRts;
    }

    private static JsonArray mergeMiddlewares(String providerName, JsonArray mws, JsonArray mergedMws) {
        if (mws == null) {
            return mergedMws;
        }

        for (int i = 0; i < mws.size(); i++) {
            final JsonObject mw = mws.getJsonObject(i);
            final String mwName = mw.getString(DynamicConfiguration.MIDDLEWARE_NAME);

            mw.put(DynamicConfiguration.MIDDLEWARE_NAME, getQualifiedName(providerName, mwName));
            mergedMws.add(mw);
        }
        return mergedMws;
    }

    private static JsonArray mergeServices(String providerName, JsonArray svs, JsonArray mergedSvs) {
        if (svs == null) {
            return mergedSvs;
        }

        for (int i = 0; i < svs.size(); i++) {
            final JsonObject sv = svs.getJsonObject(i);
            final String svName = sv.getString(DynamicConfiguration.SERVICE_NAME);

            sv.put(DynamicConfiguration.SERVICE_NAME, getQualifiedName(providerName, svName));
            mergedSvs.add(sv);
        }
        return mergedSvs;
    }

    /**
     * Returns the qualified name of a service like 'service@provider'.
     * If it already is a qualified service, this is a no-op.
     * 
     * @param providerName
     * @param name
     * @return qualifiedName
     */
    private static String getQualifiedName(String providerName, String name) {
        if (isQualifiedName(name)) {
            return name;
        }
        return makeQualifiedName(providerName, name);
    }

    /**
     * Qualified name is of the format 'service@provider'
     * 
     * @param providerName
     * @param name
     * @return qualifiedName
     */
    private static String makeQualifiedName(String providerName, String name) {
        return String.format("%s@%s", name, providerName);
    }

    private static boolean isQualifiedName(String name) {
        return name.contains("@");
    }

    @Override
    public void start(Promise<Void> startPromise) {
        listenProviders();
        listenConfigurations();

        this.vertx.deployVerticle(this.provider)
            .onSuccess(ar -> {
                startPromise.complete();
            }).onFailure(err -> {
                startPromise.fail(err);
            });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        this.vertx.cancelTimer(this.timerId);
        stopPromise.complete();
    }

    public void addListener(Listener listener) {
        LOGGER.debug("Adding listener '{}'", listener);
        if (this.configurationListeners == null) {
            this.configurationListeners = new ArrayList<>();
        }
        this.configurationListeners.add(listener);
    }

    /**
     * listenProviders receives configuration changes from the providers.
     * The configuration message then gets passed along a series of check
     * to finally end up in a throttler that sends it to listenConfigurations.
     */
    private void listenProviders() {
        LOGGER.debug("Listening for new configuration...");
        final MessageConsumer<JsonObject> configConsumer = this.eventBus.consumer(this.configurationAddress);

        configConsumer.handler(message -> onConfigurationAnnounce(message));
    }

    // handler for address: configuration-announce-address
    private void onConfigurationAnnounce(Message<JsonObject> message) {
        final JsonObject nextConfig = message.body();

        final String providerName = nextConfig.getString(Provider.PROVIDER_NAME);
        LOGGER.debug("Received next configuration from '{}'", providerName);

        preloadConfiguration(nextConfig);
    }

    private void preloadConfiguration(JsonObject nextConfig) {
        if (!nextConfig.containsKey(Provider.PROVIDER_NAME)
            || !nextConfig.containsKey(Provider.PROVIDER_CONFIGURATION)) {
            LOGGER.warn("Invalid configuration received");
            return;
        }

        final String providerName = nextConfig.getString(Provider.PROVIDER_NAME);
        final JsonObject providerConfig = nextConfig.getJsonObject(Provider.PROVIDER_CONFIGURATION);

        if (DynamicConfiguration.isEmptyConfiguration(providerConfig)) {
            LOGGER.info("Skipping empty configuration for provider '{}'", providerName);
            return;
        }

        // there is at most one config reload throttler per provider
        if (!this.providerConfigReloadThrottler.contains(providerName)) {
            this.providerConfigReloadThrottler.add(providerName);

            this.throttleProviderConfigReload(this.providersThrottleIntervalMs, providerName);
        }

        LOGGER.info("Publishing next configuration from '{}' provider", providerName);
        this.eventBus.publish(providerName, nextConfig);
    }

    /**
     * throttleProviderConfigReload throttles the configuration reload speed for a
     * single provider.
     * It will immediately publish a new configuration and then only publish the
     * next configuration after the throttle duration.
     * Note that in the case it receives N new configs in the timeframe of the
     * throttle duration after publishing, it will publish the last of the newly
     * received configurations.
     */
    private void throttleProviderConfigReload(int throttleMs, String providerConfigReloadAddress) {
        final Queue<JsonObject> nextConfigRing = QueueUtils.synchronizedQueue(new CircularFifoQueue<JsonObject>(1));
        final Queue<JsonObject> prevConfigRing = QueueUtils.synchronizedQueue(new CircularFifoQueue<JsonObject>(1));

        final MessageConsumer<JsonObject> consumer = this.eventBus.consumer(providerConfigReloadAddress);
        consumer.handler(message -> onConfigReload(message, throttleMs, nextConfigRing, prevConfigRing));
    }

    // handler for address: <provider> (e.g. file)
    private void onConfigReload(
        Message<JsonObject> message, int throttleMs, Queue<JsonObject> nextConfigRing,
        Queue<JsonObject> prevConfigRing
    ) {
        final JsonObject nextConfig = message.body();
        if (prevConfigRing.isEmpty()) {
            LOGGER.debug("Publishing initial configuration immediately");
            prevConfigRing.add(nextConfig.copy());
            nextConfigRing.add(nextConfig.copy());
            publishConfiguration(nextConfigRing);
            this.timerId = this.vertx.setPeriodic(throttleMs, tId -> {
                publishConfiguration(nextConfigRing);
            });
            return;
        }

        LOGGER.debug("Received new config for throttling");
        if (prevConfigRing.peek().equals(nextConfig)) {
            LOGGER.info("Skipping same configuration");
            return;
        }

        prevConfigRing.add(nextConfig.copy());
        nextConfigRing.add(nextConfig.copy());
    }

    private void publishConfiguration(Queue<JsonObject> nextConfigRing) {
        final JsonObject nextConfig = nextConfigRing.poll();
        if (nextConfig == null) {
            return;
        }
        LOGGER.info("Publishing configuration");
        this.eventBus.publish(CONFIG_THROTTLED_ADDRESS, nextConfig);
    }

    private void listenConfigurations() {
        LOGGER.debug("Listening for new configuration...");
        final MessageConsumer<JsonObject> throttledProviderConfigUpdateConsumer = this.eventBus.consumer(CONFIG_THROTTLED_ADDRESS);
        throttledProviderConfigUpdateConsumer.handler(message -> onThrottledConfiguration(message));
    }

    // handler for address: CONFIG_THROTTLED_ADDRESS
    private void onThrottledConfiguration(Message<JsonObject> message) {
        final JsonObject nextConfig = message.body();

        final String providerName = nextConfig.getString(Provider.PROVIDER_NAME);
        final JsonObject providerConfig = nextConfig.getJsonObject(Provider.PROVIDER_CONFIGURATION);
        if (providerConfig == null) {
            return;
        }

        this.currentConfigurations.put(providerName, providerConfig);

        final JsonObject mergedConfig = mergeConfigurations(this.currentConfigurations);
        applyEntrypoints(mergedConfig, this.defaultEntrypoints);

        DynamicConfiguration.validate(vertx, mergedConfig)
            .onSuccess(v -> {
                LOGGER.debug("Informing listeners about new configuration: '{}'", mergedConfig);
                for (Listener listener : this.configurationListeners) {
                    listener.listen(mergedConfig);
                }
                anyConfigurationPublished.set(true);
            }).onFailure(err -> {
                if (!anyConfigurationPublished.get()) {
                    // PORTAL-2109 if first configuration is invalid, fail hard
                    Runtime.fatal(vertx, "Initial configuration is invalid");
                }

                LOGGER.warn("Ignoring invalid configuration for '{}': '{}'", providerName, err.getMessage());
            });
    }
}
