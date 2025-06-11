package com.inventage.portal.gateway.proxy.listener;

import com.inventage.portal.gateway.GatewayRouterInternal;
import com.inventage.portal.gateway.proxy.config.model.DynamicModel;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and deploys the router structure after receiving a new/changed dynamic
 * configuration.
 */
public class RouterSwitchListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouterSwitchListener.class);

    private static final String NAME = "RouterSwitchListener";

    private final GatewayRouterInternal router;
    private final RouterFactory routerFactory;

    public RouterSwitchListener(GatewayRouterInternal router, RouterFactory routerFactory) {
        this.router = router;
        this.routerFactory = new RouterFactory(routerFactory);
    }

    @Override
    public void listen(DynamicModel model) {
        routerFactory.createRouter(model)
            .onSuccess(this::setSubRouter)
            .onFailure(err -> LOGGER.warn("Failed to create new router from config '{}': '{}'", model, err.getMessage()));
    }

    @Override
    public String toString() {
        return NAME;
    }

    private void setSubRouter(Router subRouter) {
        // TODO might this create a connection gap?
        router.clear();
        router.mountSubRouter("/", subRouter);
    }

}
