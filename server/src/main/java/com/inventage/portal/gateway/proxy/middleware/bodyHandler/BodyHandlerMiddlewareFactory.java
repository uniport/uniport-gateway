package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BodyHandlerMiddlewareFactory implements MiddlewareFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(BodyHandlerMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BODY_HANDLER;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_BODY_HANDLER);
        return Future.succeededFuture(new BodyHandlerMiddleware(vertx, name));
    }
}
