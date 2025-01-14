package com.inventage.portal.gateway.proxy.middleware.checkRoute;

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
 * Factory for {@link CheckRouteMiddleware}.
 */
public class CheckRouteMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_CHECK_ROUTE = "checkRoute";
    public static final String MIDDLEWARE_CHECK_ROUTE_PATH = "_check-route_";

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRouteMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_CHECK_ROUTE;
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
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_CHECK_ROUTE);
        return Future.succeededFuture(new CheckRouteMiddleware(name));
    }

}
