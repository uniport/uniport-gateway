package com.inventage.portal.gateway.proxy.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.listener.Listener;
import com.inventage.portal.gateway.proxy.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * It listens to incoming dynamic configurations. Upon passing several checks is passed to all
 * registered listeners. A namespace per provider exists to avoid clashes.
 */
public class ConfigurationWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationWatcher.class);

    private Vertx vertx;

    private Provider provider;

    private String configurationAddress;

    private Map<String, JsonObject> currentConfigurations;

    // TODO use providersThrottleDuration
    private int providersThrottleDuration;

    private List<Listener> configurationListeners;

    private List<String> defaultEntrypoints;

    public ConfigurationWatcher(Vertx vertx, Provider provider, String configurationAddress,
            int providersThrottleDuration, List<String> defaultEntrypoints) {
        LOGGER.trace("construcutor");
        this.vertx = vertx;
        this.provider = provider;
        this.configurationAddress = configurationAddress;
        this.providersThrottleDuration = providersThrottleDuration;
        this.defaultEntrypoints = defaultEntrypoints;
        this.currentConfigurations = new HashMap<>();
    }

    public Future<String> start() {
        LOGGER.trace("start");
        listenProviders();
        return this.vertx.deployVerticle(this.provider);
    }

    public void addListener(Listener listener) {
        LOGGER.trace("addListener");
        if (this.configurationListeners == null) {
            this.configurationListeners = new ArrayList<>();
        }
        this.configurationListeners.add(listener);
    }

    private void listenProviders() {
        LOGGER.trace("listenProviders");
        EventBus eb = this.vertx.eventBus();
        MessageConsumer<JsonObject> configConsumer = eb.consumer(this.configurationAddress);

        configConsumer.handler(message -> {
            JsonObject messageBody = message.body();

            String providerName = messageBody.getString(Provider.PROVIDER_NAME);
            JsonObject providerConfig = messageBody.getJsonObject(Provider.PROVIDER_CONFIGURATION);
            LOGGER.debug("listenProviders: Received new configuration from '{}'", providerName);

            loadMessage(providerName, providerConfig);
        });
    }

    private void loadMessage(String providerName, JsonObject providerConfig) {
        LOGGER.trace("loadMessage");
        if (isEmptyConfiguration(providerConfig)) {
            LOGGER.info("loadMessage: Skipping empty configuration for provider %s", providerName);
            return;
        }

        this.currentConfigurations.put(providerName, providerConfig);

        JsonObject mergedConfig = mergeConfigurations(this.currentConfigurations);
        applyEntrypoints(mergedConfig, this.defaultEntrypoints);

        LOGGER.debug("loadMessage: Informing listeners about new configuration '{}'", mergedConfig);
        for (Listener listener : this.configurationListeners) {
            listener.listen(mergedConfig);
        }
    }

    // TODO introduce provider namespaces
    private static JsonObject mergeConfigurations(Map<String, JsonObject> configurations) {
        LOGGER.trace("mergeConfigurations");
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
                for (int i = 0; i < rts.size(); i++) {
                    JsonObject rt = rts.getJsonObject(i);
                    String rtName = rt.getString(DynamicConfiguration.ROUTER_NAME);

                    // TODO maybe use map like traefik
                    // rt.put(DynamicConfiguration.ROUTER_NAME,
                    // makeQualifiedName(providerName, rtName));
                    mergedRts.add(rt);
                }

                JsonArray mws = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                JsonArray mergedMws =
                        mergedHttpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                for (int i = 0; i < mws.size(); i++) {
                    JsonObject mw = mws.getJsonObject(i);
                    String mwName = mw.getString(DynamicConfiguration.MIDDLEWARE_NAME);

                    // TODO maybe use map like traefik
                    // mw.put(DynamicConfiguration.MIDDLEWARE_NAME,
                    // makeQualifiedName(providerName, mwName));
                    mergedMws.add(mw);
                }

                JsonArray svs = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
                JsonArray mergedSvs = mergedHttpConfig.getJsonArray(DynamicConfiguration.SERVICES);
                for (int i = 0; i < svs.size(); i++) {
                    JsonObject sv = svs.getJsonObject(i);
                    String svName = sv.getString(DynamicConfiguration.SERVICE_NAME);

                    // TODO maybe use map like traefik
                    // sv.put(DynamicConfiguration.SERVICE_NAME,
                    // makeQualifiedName(providerName, svName));
                    mergedSvs.add(sv);
                }
            }
        }

        return mergedConfig;
    }

    private static String makeQualifiedName(String providerName, String routerName) {
        LOGGER.trace("makeQualifiedName");
        return String.format("%s@%s", routerName, providerName);
    }

    private static JsonObject applyEntrypoints(JsonObject config, List<String> entrypoints) {
        LOGGER.trace("applyEntrypoints");
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

    private static boolean isEmptyConfiguration(JsonObject config) {
        LOGGER.trace("isEmptyConfiguration");
        if (config == null) {
            return true;
        }

        JsonObject httpConfig = config.getJsonObject(DynamicConfiguration.HTTP);
        if (httpConfig == null) {
            return true;
        }

        JsonArray httpRouters = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray httpMiddlewares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonArray httpServices = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        Boolean httpEmpty = httpRouters == null && httpMiddlewares == null && httpServices == null;

        return httpEmpty;
    }
}
