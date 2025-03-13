package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;

/**
 * Factory for {@link PreventForeignInitiatedAuthMiddleware}.
 */
public class PreventForeignInitiatedAuthMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "checkInitiatedAuth";
    public static final String REDIRECT_URI = "redirectUri";

    // defaults
    public static final String DEFAULT_REDIRECT_URI = "/";

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(REDIRECT_URI, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_REDIRECT_URI))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<PreventForeignInitiatedAuthMiddlewareOptions> modelType() {
        return PreventForeignInitiatedAuthMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final PreventForeignInitiatedAuthMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}' of type '{}' middleware successfully", name, TYPE);
        return Future.succeededFuture(
            new PreventForeignInitiatedAuthMiddleware(name, options.getRedirectURI()));
    }
}
