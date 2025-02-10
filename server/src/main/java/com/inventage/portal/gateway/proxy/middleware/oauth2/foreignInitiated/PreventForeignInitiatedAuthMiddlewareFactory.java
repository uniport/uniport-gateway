package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;

/**
 * Factory for {@link PreventForeignInitiatedAuthMiddleware}.
 */
public class PreventForeignInitiatedAuthMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String PREVENT_FOREIGN_INITIATED_AUTHENTICATION = "checkInitiatedAuth";
    public static final String PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT = "redirectUri";

    private static final String DEFAULT_REDIRECT = null;

    @Override
    public String provides() {
        return PREVENT_FOREIGN_INITIATED_AUTHENTICATION;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, ONE))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT, DEFAULT_REDIRECT);

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' of type '{}' middleware successfully", name, PREVENT_FOREIGN_INITIATED_AUTHENTICATION);
        final String redirect = middlewareConfig.getString(PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT);
        return Future.succeededFuture(new PreventForeignInitiatedAuthMiddleware(name, redirect));
    }
}
