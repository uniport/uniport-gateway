package com.inventage.portal.gateway.proxy.middleware.authorization.authorizationBearer;

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
 * Factory for {@link AuthorizationBearerMiddleware}.
 */
public class AuthorizationBearerMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String AUTHORIZATION_BEARER = "authorizationBearer";
    public static final String AUTHORIZATION_BEARER_SESSION_SCOPE = "sessionScope";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddlewareFactory.class);

    @Override
    public String provides() {
        return AUTHORIZATION_BEARER;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(AUTHORIZATION_BEARER_SESSION_SCOPE, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", AUTHORIZATION_BEARER);
        return Future.succeededFuture(new AuthorizationBearerMiddleware(
            vertx,
            name,
            middlewareConfig.getString(AUTHORIZATION_BEARER_SESSION_SCOPE)));
    }

}
