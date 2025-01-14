package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link BodyHandlerMiddleware}.
 */
public class BodyHandlerMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_BODY_HANDLER = "bodyHandler";

    private static final Logger LOGGER = LoggerFactory.getLogger(BodyHandlerMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_BODY_HANDLER;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", MIDDLEWARE_BODY_HANDLER);
        return Future.succeededFuture(new BodyHandlerMiddleware(vertx, name));
    }
}
