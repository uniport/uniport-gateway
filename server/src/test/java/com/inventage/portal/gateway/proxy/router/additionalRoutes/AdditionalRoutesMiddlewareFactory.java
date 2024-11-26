package com.inventage.portal.gateway.proxy.router.additionalRoutes;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for adding the additional routes to the router
 */
public class AdditionalRoutesMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalRoutesMiddlewareFactory.class);

    @Override
    public String provides() {
        return "additionalRoutes";
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", provides());

        final String path = middlewareConfig.getString("path", "/some-path");
        router.route().path(path).handler(ctx -> {
            LOGGER.debug("I'm a teapot");
            ctx.response().setStatusCode(418).end();
        });

        return Future.succeededFuture(new AdditionalRoutesMiddleware(name));
    }
}
