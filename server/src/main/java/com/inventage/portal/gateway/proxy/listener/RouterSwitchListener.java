package com.inventage.portal.gateway.proxy.listener;

import com.inventage.portal.gateway.proxy.router.RouterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * Builds and deploys the router structure after receiving a new/changed dynamic configuration.
 */
public class RouterSwitchListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouterSwitchListener.class);

    private Router router;
    private RouterFactory routerFactory;

    public RouterSwitchListener(Router router, RouterFactory routerFactory) {
        this.router = router;
        this.routerFactory = routerFactory;
    }

    @Override
    public void listen(JsonObject config) {
        Future<Router> routerCreation = routerFactory.createRouter(config);
        routerCreation.onComplete(ar -> {
            if (ar.succeeded()) {
                setSubRouter(ar.result());
            } else {
                LOGGER.warn("Failed to create new router with '{}' from config '{}'", ar.cause(),
                        config);
            }
        });

    }

    private void setSubRouter(Router subRouter) {
        // TODO might this create a connection gap?
        this.router.clear();
        this.router.mountSubRouter("/", subRouter);
    }

}
