package com.inventage.portal.gateway.proxy.router.additionalRoutes;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for adding the additional routes to the router
 */
public class AdditionalRoutesMiddlewareFactory implements MiddlewareFactory {

    public static final String ADDITIONAL_ROUTES = "additionalRoutes";
    public static final String ADDITIONAL_ROUTES_PATH = "path";

    public static final String DEFAULT_ADDITIONAL_ROUTES_PATH = "/some-path";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalRoutesMiddlewareFactory.class);

    @Override
    public String provides() {
        return ADDITIONAL_ROUTES;
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
    public Class<AdditionalRoutesMiddlewareOptions> modelType() {
        return AdditionalRoutesMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final AdditionalRoutesMiddlewareOptions options = castOptions(config, modelType());

        router.route()
            .path(options.getPath())
            .handler(ctx -> {
                LOGGER.debug("I'm a teapot");
                ctx.response()
                    .setStatusCode(418)
                    .end();
            });

        LOGGER.info("Created '{}' middleware successfully", ADDITIONAL_ROUTES);
        return Future.succeededFuture(
            new AdditionalRoutesMiddleware(name));
    }
}
