package com.inventage.portal.gateway.proxy;

import java.util.Arrays;
import java.util.Optional;
import com.inventage.portal.gateway.core.PortalGatewayVerticle;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.ConfigurationWatcher;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.listener.RouterSwitchListener;
import com.inventage.portal.gateway.proxy.provider.aggregator.ProviderAggregator;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * Application for the proxy feature of the portal gateway. The routers will be read from the
 * incoming configurations provided by different providers.
 */
public class ProxyApplication implements Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    /**
     * the name of this instance
     */
    private final String name;

    /**
     * the public hostname of this application
     */
    private String publicHostname;

    /**
     * the name of the entrypoint this application should be mounted on
     */
    private final String entrypoint;

    /**
     * the port of the entrypoint this application should be mounted on
     */
    private String entrypointPort;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private String rootPath = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private Router router;

    /**
     * the providers that should be launched for this applicaiton to retrieve the dynamic
     * configuration
     */
    private JsonArray providers;

    private JsonObject env;

    private int providersThrottleDuration;

    public ProxyApplication(String name, String entrypoint, JsonObject staticConfig, Vertx vertx) {
        LOGGER.trace("construcutor");
        this.name = name;
        this.entrypoint = entrypoint;
        this.router = Router.router(vertx);

        this.providers = staticConfig.getJsonArray(StaticConfiguration.PROVIDERS);
        this.providersThrottleDuration =
                staticConfig.getInteger(StaticConfiguration.PROVIDERS_THROTTLE_INTERVAL_SEC, 2);

        this.env = staticConfig.copy();
        this.env.remove(StaticConfiguration.ENTRYPOINTS);
        this.env.remove(StaticConfiguration.APPLICATIONS);
        this.env.remove(StaticConfiguration.PROVIDERS);

        this.publicHostname = env.getString(PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME,
                PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);
        this.entrypointPort = DynamicConfiguration
                .getObjByKeyWithValue(staticConfig.getJsonArray(StaticConfiguration.ENTRYPOINTS),
                        StaticConfiguration.ENTRYPOINT_NAME, this.entrypoint)
                .getString(StaticConfiguration.ENTRYPOINT_PORT);
    }

    public String toString() {
        LOGGER.trace("toString");
        return String.format("%s:%s", getClass().getSimpleName(), name);
    }

    @Override
    public String rootPath() {
        LOGGER.trace("rootPath");
        return rootPath;
    }

    @Override
    public Optional<Router> router() {
        LOGGER.trace("router");
        return Optional.of(router);
    }

    @Override
    public String entrypoint() {
        LOGGER.trace("entrypoint");
        return entrypoint;
    }

    @Override
    public Future<?> deployOn(Vertx vertx) {
        LOGGER.trace("deployOn");
        String configurationAddress = "configuration-announce-address";

        ProviderAggregator aggregator =
                new ProviderAggregator(vertx, configurationAddress, providers, this.env);

        ConfigurationWatcher watcher =
                new ConfigurationWatcher(vertx, aggregator, configurationAddress,
                        this.providersThrottleDuration, Arrays.asList(this.entrypoint));

        RouterFactory routerFactory = new RouterFactory(vertx, publicHostname, entrypointPort);

        watcher.addListener(new RouterSwitchListener(this.router, routerFactory));

        return watcher.start();
    }

}
