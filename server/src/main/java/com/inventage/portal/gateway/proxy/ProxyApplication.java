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
 * "routers" configuration key.
 */
public class ProxyApplication implements Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    /**
     * the name of this instance
     */
    private final String name;

    /**
     * the name of the entrypoint this application should be mounted on
     */
    private final String entrypoint;

    private String entrypointPort;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private String rootPath = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private Router router;

    private JsonArray providers;

    private String publicHostname;

    private final int providersThrottleDuration = 2;

    public ProxyApplication(String name, String entrypoint, JsonObject staticConfig, Vertx vertx) {
        this.name = name;
        this.entrypoint = entrypoint;
        this.router = Router.router(vertx);
        this.providers = staticConfig.getJsonArray(StaticConfiguration.PROVIDERS);
        this.publicHostname =
                staticConfig.getString(PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME,
                        PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);
        this.entrypointPort = DynamicConfiguration
                .getObjByKeyWithValue(staticConfig.getJsonArray(StaticConfiguration.ENTRYPOINTS),
                        StaticConfiguration.ENTRYPOINT_NAME, this.entrypoint)
                .getString(StaticConfiguration.ENTRYPOINT_PORT);

    }

    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), name);
    }

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public Optional<Router> router() {
        return Optional.of(router);
    }

    @Override
    public String entrypoint() {
        return entrypoint;
    }

    @Override
    public Future<?> deployOn(Vertx vertx) {
        String configurationAddress = "configuration-announce-address";

        ProviderAggregator aggregator =
                new ProviderAggregator(vertx, configurationAddress, providers);

        ConfigurationWatcher watcher =
                new ConfigurationWatcher(vertx, aggregator, configurationAddress,
                        this.providersThrottleDuration, Arrays.asList(this.entrypoint));

        RouterFactory routerFactory = new RouterFactory(vertx, publicHostname, entrypointPort);

        watcher.addListener(new RouterSwitchListener(this.router, routerFactory));

        return watcher.start();
    }

}
