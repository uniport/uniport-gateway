package com.inventage.portal.gateway.proxy.middleware.cors;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for adding the CorsHandler of Vert.x.
 * see https://vertx.io/docs/vertx-web/java/#_cors_handling
 * see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin
 */
public class CorsMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorsMiddleware.class);

    private final String name;
    private final CorsHandler corsHandler;

    public CorsMiddleware(String name, String allowedOrigin) {
        this.name = name;
        this.corsHandler = CorsHandler.create().addOrigin(allowedOrigin);
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        corsHandler.handle(ctx);
    }
}
