package com.inventage.portal.gateway.core.entrypoint;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.log.RequestResponseLogger;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Entry point for the portal gateway.
 */
public class Entrypoint {

    // keys in the portal-gateway.json

    public static final String ENTRYPOINTS = "entrypoints";
    public static final String NAME = "name";
    public static final String PORT = "port";

    private static Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

    private final Vertx vertx;
    private final String name;
    private final int port;
    private Router router;
    private boolean enabled;
    private Tls tls;

    public Entrypoint(String name, int port, Vertx vertx) {
        this.vertx = vertx;
        this.name = name;
        this.port = port;
        this.enabled = true;
    }

    public Object name() {
        return name;
    }

    public int port() {
        return port;
    }

    public Router router() {
        if (router == null) {
            router = Router.router(vertx);
            router.route().handler(RequestResponseLogger.create());
            router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        }
        return router;
    }

//    private Router configureGlobalRoutes(Router router, JsonObject config) {
//        routingContextHandlerProviderLoader.findFirst().ifPresent(routingContextHandlerProvider -> {
//            final List<Handler<RoutingContext>> handlers = routingContextHandlerProvider.handlers(vertx, config);
//            if (handlers != null) {
//                handlers.stream().forEach(handler -> router.route().handler(handler));
//            }
//        });
//        return router;
//    }


    public void mount(Application application) {
        final Optional<Router> optionApplicationRouter = application.router();
        optionApplicationRouter.ifPresent(
                applicationRouter -> {
                    if (name.equals(application.entrypoint())) {
                        if (enabled()) {
                            router().mountSubRouter(application.rootPath(), applicationRouter);
                            LOGGER.info("mount: application '{}' for '{}' at endpoint '{}'", application, application.rootPath(), name);
                        }
                        else {
                            LOGGER.warn("mount: disabled endpoint '{}' can not mount application '{}' for '{}'", name, application, application.rootPath());
                        }
                    }
                });
    }

    public boolean isTls() {
        return tls != null;
    }

    public JksOptions jksOptions() {
        if (isTls()) {
            return tls.jksOptions();
        }
        return new JksOptions();
    }

    public boolean enabled() {
        return enabled && port > 0;
    }

    public void disable() {
        enabled = false;
    }

    class Tls {

        public JksOptions jksOptions() {
            return null;
//            final String keyStorePath = config.getString(CONFIG_PREFIX + CONFIG_HTTPS_KEY_STORE_PATH);
//            if (keyStorePath == null || keyStorePath.isEmpty()) {
//                throw new IllegalStateException("When using https the path to the key store must be configured by variable: '" +
//                        CONFIG_PREFIX + "https-key-store-path'. To disable the https port configuration use '-1' as port.");
//            }
//            return new JksOptions()
//                    .setPath(keyStorePath)
//                    .setPassword(config.getString(CONFIG_PREFIX + CONFIG_HTTPS_KEY_STORE_PASSWORD));
        }
    }
}
