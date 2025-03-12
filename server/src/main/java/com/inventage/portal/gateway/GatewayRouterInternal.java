package com.inventage.portal.gateway;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayRouterInternal extends RouterImpl {

    private static final String ROUTER_NAME_KEY = "name";
    private static final String ROUTER_PARENT_KEY = "parent";
    private static final String ROUTER_LEVEL_KEY = "level";

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRouterInternal.class);

    public static GatewayRouterInternal router(Vertx vertx, String name) {
        return (GatewayRouterInternal) new GatewayRouterInternal(vertx)
            .putMetadata(ROUTER_NAME_KEY, name);
    }

    public GatewayRouterInternal(Vertx vertx) {
        super(vertx);
    }

    public String getName() {
        return getMetadata(ROUTER_NAME_KEY);
    }

    @Override
    public Route mountSubRouter(String mountPoint, Router subRouter) {
        if (mountPoint.endsWith("*")) {
            throw new IllegalArgumentException("Don't include * when mounting a sub router");
        }

        final String routerName = getMetadata(ROUTER_NAME_KEY);
        final String subRouterName = subRouter.getMetadata(ROUTER_NAME_KEY);
        final String routeName = String.format("subRouter %s", subRouterName);
        subRouter.putMetadata(ROUTER_PARENT_KEY, routerName);

        int routerLevel = 0;
        if (metadata().containsKey(ROUTER_LEVEL_KEY)) {
            routerLevel = getMetadata(ROUTER_LEVEL_KEY);
        }
        final int subRouterLevel = routerLevel + 1;
        subRouter.putMetadata(ROUTER_LEVEL_KEY, subRouterLevel);

        final Route route = route(mountPoint + "*")
            .setName(routeName)
            .subRouter(subRouter);

        if (!metadata().containsKey(ROUTER_PARENT_KEY)) {
            // special-case: top-level router
            logRouterStructure(routerName, null, routerLevel, getRoutes());
        }
        logRouterStructure(subRouterName, routerName, subRouterLevel, subRouter.getRoutes());

        return route;
    }

    private static void logRouterStructure(String name, String parent, int level, List<Route> routes) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("Router '%s' (parent '%s')", name, parent));

        if (routes.isEmpty()) {
            builder.append("\n\tNo routes");
        }
        for (Route r : routes) {
            builder.append(String.format("\n\tRoute '%s', methods '%s', path '%s'",
                r.getName(), r.methods(), r.getPath()));
        }
        LOGGER.debug(builder.toString());
    }

}
