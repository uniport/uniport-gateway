package com.inventage.portal.gateway.proxy.middleware.checkRoute;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.HttpResponder;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for checking a specific route.
 * If "_check-route_" is found within in the URI of the incoming request, the request is
 * ended immediately and returns a status code 202. Otherwise, the request is passed on.
 */
public class CheckRouteMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRouteMiddleware.class);

    private static final int STATUS_CODE = 202; // Accepted

    @Override
    public void handle(RoutingContext ctx) {
        if (isCheckRoute(ctx)) {
            LOGGER.info("done for URL '{}' with status code '{}'", ctx.request().absoluteURI(), STATUS_CODE);
            HttpResponder.respondWithStatusCode(STATUS_CODE, ctx);
        }
        else {
            ctx.next();
        }
    }

    private boolean isCheckRoute(RoutingContext ctx) {
        return ctx.request().uri().contains(DynamicConfiguration.MIDDLEWARE_CHECK_ROUTE_PATH);
    }

}
