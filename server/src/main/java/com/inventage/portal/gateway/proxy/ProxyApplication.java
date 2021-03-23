package com.inventage.portal.gateway.proxy;

import java.util.Arrays;
import java.util.Optional;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.provider.aggregator.ProviderAggregator;
import com.inventage.portal.gateway.core.router.RouterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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

    /** the global configuration */
    private final JsonObject staticConfig;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private String rootPath = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private Router router;

    private final int providersThrottleDuration = 2;

    public ProxyApplication(String name, String entrypoint, JsonObject staticConfig, Vertx vertx) {
        this.name = name;
        this.entrypoint = entrypoint;
        this.staticConfig = staticConfig;
        this.router = Router.router(vertx);
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
                new ProviderAggregator(vertx, configurationAddress, staticConfig);

        ConfigurationWatcher watcher =
                new ConfigurationWatcher(vertx, aggregator, configurationAddress,
                        this.providersThrottleDuration, Arrays.asList(this.entrypoint));

        RouterFactory routerFactory = new RouterFactory(vertx);

        watcher.addListener(switchRouter(vertx, routerFactory));

        return watcher.start();
    }

    private Listener switchRouter(Vertx vertx, RouterFactory routerFactory) {
        return new Listener() {
            @Override
            public void listen(JsonObject config) {
                Future<Router> routerCreation = routerFactory.createRouter(config);
                routerCreation.onComplete(ar -> {
                    if (ar.succeeded()) {
                        setSubRouter(ar.result());
                    } else {
                        LOGGER.error("Failed to create new router with '{}' from config '{}'",
                                ar.cause(), config);
                    }
                });
            }
        };
    }

    private void setSubRouter(Router subRouter) {
        // TODO might this create a connection gap?
        this.router.clear();
        this.router.mountSubRouter("/", subRouter);
    }
}
