package com.inventage.portal.gateway.proxy.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.listener.Listener;
import com.inventage.portal.gateway.proxy.provider.Provider;
import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * It listens to incoming dynamic configurations. Upon passing several checks is passed to all
 * registered listeners. A namespace per provider exists to avoid clashes.
 */
public class ConfigurationWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationWatcher.class);

    private static final String CONFIG_VALIDATED_ADDRESS = "configuration-watcher-config-validated";

    private Vertx vertx;

    private EventBus eventBus;

    private Provider provider;

    private String configurationAddress;

    private Map<String, JsonObject> currentConfigurations;

    private int providersThrottleIntervalSec;

    private List<Listener> configurationListeners;

    private List<String> defaultEntrypoints;

    private Set<String> providerConfigReloadThrottler;

    public ConfigurationWatcher(Vertx vertx, Provider provider, String configurationAddress,
            int providersThrottleIntervalSec, List<String> defaultEntrypoints) {
        this.vertx = vertx;
        this.eventBus = vertx.eventBus();
        this.provider = provider;
        this.configurationAddress = configurationAddress;
        this.providersThrottleIntervalSec = providersThrottleIntervalSec;
        this.defaultEntrypoints = defaultEntrypoints;
        this.currentConfigurations = new HashMap<>();
        this.providerConfigReloadThrottler = new HashSet<>();
    }

    public Future<String> start() {
        listenProviders();
        listenConfigurations();
        return this.vertx.deployVerticle(this.provider);
    }

    public void addListener(Listener listener) {
        LOGGER.debug("addListener: listener '{}'", listener);
        if (this.configurationListeners == null) {
            this.configurationListeners = new ArrayList<>();
        }
        this.configurationListeners.add(listener);
    }

    // listenProviders receives configuration changes from the providers.
    // The configuration message then gets passed along a series of check
    // to finally end up in a throttler that sends it to listenConfigurations.
    private void listenProviders() {
        LOGGER.debug("listenProviders");
        MessageConsumer<JsonObject> configConsumer =
                this.eventBus.consumer(this.configurationAddress);

        configConsumer.handler(message -> onConfigurationAnnounce(message));
    }

    // handler for address: configuration-announce-address
    private void onConfigurationAnnounce(Message<JsonObject> message) {
        JsonObject nextConfig = message.body();

        String providerName = nextConfig.getString(Provider.PROVIDER_NAME);
        LOGGER.debug("onConfigurationAnnounce: received next configuration from '{}'",
                providerName);

        preloadConfiguration(nextConfig);
    }

    private void preloadConfiguration(JsonObject nextConfig) {
        if (!nextConfig.containsKey(Provider.PROVIDER_NAME)
                || !nextConfig.containsKey(Provider.PROVIDER_CONFIGURATION)) {
            LOGGER.warn("preloadConfiguration: invalid configuration received");
            return;
        }

        String providerName = nextConfig.getString(Provider.PROVIDER_NAME);
        JsonObject providerConfig = nextConfig.getJsonObject(Provider.PROVIDER_CONFIGURATION);

        if (DynamicConfiguration.isEmptyConfiguration(providerConfig)) {
            LOGGER.info("preloadConfiguration: skipping empty configuration for provider '{}'",
                    providerName);
            return;
        }

        // there is at most one config reload throttler per provider
        if (!this.providerConfigReloadThrottler.contains(providerName)) {
            this.providerConfigReloadThrottler.add(providerName);

            this.throttleProviderConfigReload(this.providersThrottleIntervalSec, providerName);
        }

        LOGGER.info("preloadConfiguration: publishing next configuration from '{}' provider",
                providerName);
        this.eventBus.publish(providerName, nextConfig);
    }


    // throttleProviderConfigReload throttles the configuration reload speed for a single provider.
    // It will immediately publish a new configuration and then only publish the next configuration
    // after the throttle duration.
    // Note that in the case it receives N new configs in the timeframe of the throttle duration
    // after publishing, it will publish the last of the newly received configurations.
    private void throttleProviderConfigReload(int throttleSec, String providerConfigReloadAddress) {
        Queue<JsonObject> ring = QueueUtils.synchronizedQueue(new CircularFifoQueue<JsonObject>(1));

        this.vertx.setPeriodic(throttleSec * 1000, timerID -> {
            JsonObject nextConfig = ring.poll();
            if (nextConfig == null) {
                return;
            }
            LOGGER.info("throttleProviderConfigReload: publishing configuration");
            this.eventBus.publish(CONFIG_VALIDATED_ADDRESS, nextConfig);
        });

        MessageConsumer<JsonObject> consumer = this.eventBus.consumer(providerConfigReloadAddress);
        consumer.handler(message -> onConfigReload(message, ring));
    }

    // handler for address: <provider> (e.g. file)
    private void onConfigReload(Message<JsonObject> message, Queue<JsonObject> ring) {
        JsonObject nextConfig = message.body();
        JsonObject previousConfig = ring.peek();
        if (previousConfig == null) {
            ring.offer(nextConfig.copy());
            return;
        }

        if (previousConfig.equals(nextConfig)) {
            LOGGER.info("onConfigReload: skipping same configuration");
            return;
        }

        ring.offer(nextConfig.copy());
    }

    private void listenConfigurations() {
        LOGGER.debug("listenConfigurations");
        MessageConsumer<JsonObject> validatedProviderConfigUpdateConsumer =
                this.eventBus.consumer(CONFIG_VALIDATED_ADDRESS);

        validatedProviderConfigUpdateConsumer.handler(message -> onValidConfiguration(message));
    }

    // handler for address: CONFIG_VALIDATED_ADDRESS
    private void onValidConfiguration(Message<JsonObject> message) {
        JsonObject nextConfig = message.body();

        String providerName = nextConfig.getString(Provider.PROVIDER_NAME);
        JsonObject providerConfig = nextConfig.getJsonObject(Provider.PROVIDER_CONFIGURATION);

        if (providerConfig == null) {
            return;
        }

        this.currentConfigurations.put(providerName, providerConfig);

        JsonObject mergedConfig = mergeConfigurations(this.currentConfigurations);
        applyEntrypoints(mergedConfig, this.defaultEntrypoints);


        DynamicConfiguration.validate(vertx, mergedConfig, true).onSuccess(handler -> {
            LOGGER.debug("onValidConfiguration: Informing listeners about new configuration '{}'",
                    mergedConfig);
            for (Listener listener : this.configurationListeners) {
                listener.listen(mergedConfig);
            }
        }).onFailure(err -> {
            LOGGER.warn(
                    "onValidConfiguration: Ignoring invalid configuration for '{}' because of '{}'",
                    providerName, err.getMessage());
        });
    }

    private static JsonObject applyEntrypoints(JsonObject config, List<String> entrypoints) {
        JsonObject httpConfig = config.getJsonObject(DynamicConfiguration.HTTP);

        if (httpConfig == null) {
            return config;
        }

        JsonArray rs = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        for (int i = 0; i < rs.size(); i++) {
            JsonObject r = rs.getJsonObject(i);
            JsonArray rEntrypoints = r.getJsonArray(DynamicConfiguration.ROUTER_ENTRYPOINTS);
            if (rEntrypoints == null || rEntrypoints.size() == 0) {
                LOGGER.info(
                        "applyEntrypoints: No entryPoint defined for this router, using the default one(s) instead '{}'",
                        entrypoints.toString());
                r.put(DynamicConfiguration.ROUTER_ENTRYPOINTS, new JsonArray(entrypoints));
            }
        }

        return config;
    }

    private static JsonObject mergeConfigurations(Map<String, JsonObject> configurations) {
        LOGGER.debug("mergeConfigurations");
        JsonObject mergedConfig = DynamicConfiguration.buildDefaultConfiguration();
        JsonObject mergedHttpConfig = mergedConfig.getJsonObject(DynamicConfiguration.HTTP);

        if (mergedHttpConfig == null) {
            return mergedConfig;
        }

        Set<String> providerNames = configurations.keySet();
        for (String providerName : providerNames) {
            JsonObject conf = configurations.get(providerName);
            JsonObject httpConf = conf.getJsonObject(DynamicConfiguration.HTTP);

            if (httpConf != null) {
                JsonArray rts = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
                JsonArray mergedRts = mergedHttpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
                mergeRouters(providerName, rts, mergedRts);

                JsonArray mws = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                JsonArray mergedMws =
                        mergedHttpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                mergeMiddlewares(providerName, mws, mergedMws);

                JsonArray svs = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
                JsonArray mergedSvs = mergedHttpConfig.getJsonArray(DynamicConfiguration.SERVICES);
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
            JsonObject rt = rts.getJsonObject(i);

            String rtName = rt.getString(DynamicConfiguration.ROUTER_NAME);
            rt.put(DynamicConfiguration.ROUTER_NAME, getQualifiedName(providerName, rtName));

            // Service and middlewares may referecing to another provider namespace
            // The names are only patched if this is not the case.
            String svName = rt.getString(DynamicConfiguration.ROUTER_SERVICE);
            rt.put(DynamicConfiguration.ROUTER_SERVICE, getQualifiedName(providerName, svName));

            JsonArray mwNames = rt.getJsonArray(DynamicConfiguration.ROUTER_MIDDLEWARES);
            if (mwNames != null) {
                JsonArray qualifiedMwNames = new JsonArray();
                for (int j = 0; j < mwNames.size(); j++) {
                    String mwName = mwNames.getString(j);
                    qualifiedMwNames.add(getQualifiedName(providerName, mwName));
                }
                rt.put(DynamicConfiguration.ROUTER_MIDDLEWARES, qualifiedMwNames);

            }

            mergedRts.add(rt);
        }
        return mergedRts;
    }

    private static JsonArray mergeMiddlewares(String providerName, JsonArray mws,
            JsonArray mergedMws) {
        if (mws == null) {
            return mergedMws;
        }

        for (int i = 0; i < mws.size(); i++) {
            JsonObject mw = mws.getJsonObject(i);
            String mwName = mw.getString(DynamicConfiguration.MIDDLEWARE_NAME);

            mw.put(DynamicConfiguration.MIDDLEWARE_NAME, getQualifiedName(providerName, mwName));
            mergedMws.add(mw);
        }
        return mergedMws;
    }

    private static JsonArray mergeServices(String providerName, JsonArray svs,
            JsonArray mergedSvs) {
        if (svs == null) {
            return mergedSvs;
        }

        for (int i = 0; i < svs.size(); i++) {
            JsonObject sv = svs.getJsonObject(i);
            String svName = sv.getString(DynamicConfiguration.SERVICE_NAME);

            sv.put(DynamicConfiguration.SERVICE_NAME, getQualifiedName(providerName, svName));
            mergedSvs.add(sv);
        }
        return mergedSvs;
    }

    private static String getQualifiedName(String providerName, String name) {
        if (isQualifiedName(name)) {
            return name;
        }
        return makeQualifiedName(providerName, name);
    }

    private static String makeQualifiedName(String providerName, String name) {
        return String.format("%s@%s", name, providerName);
    }

    private static boolean isQualifiedName(String name) {
        return name.contains("@");
    }
}
