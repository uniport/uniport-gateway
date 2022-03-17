package com.inventage.portal.gateway.proxy.middleware.cors;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * Middleware for adding the CorsHandler of Vert.x.
 * see https://vertx.io/docs/vertx-web/java/#_cors_handling
 * see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin
 */
public class CorsMiddleware implements Middleware {

    public CorsMiddleware(Router router, String allowedOrigin) {
        router.route().handler(CorsHandler.create(allowedOrigin));
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.next();
    }
}
