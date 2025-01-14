package com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link PassAuthorizationMiddleware}.
 */
public class PassAuthorizationMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    // schema
    public static final String MIDDLEWARE_PASS_AUTHORIZATION = "passAuthorization";
    public static final String MIDDLEWARE_PASS_AUTHORIZATION_SESSION_SCOPE = "sessionScope";

    private static final Logger LOGGER = LoggerFactory.getLogger(PassAuthorizationMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_PASS_AUTHORIZATION;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return super.optionsSchema()
            .property(MIDDLEWARE_PASS_AUTHORIZATION_SESSION_SCOPE, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH));
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return super.validate(options)
            .compose(v -> {
                final String sessionScope = options.getString(MIDDLEWARE_PASS_AUTHORIZATION_SESSION_SCOPE);
                if (sessionScope == null || sessionScope.length() == 0) {
                    return Future.failedFuture("No session scope defined");
                }
                return Future.succeededFuture();
            });
    }

    @Override
    protected Middleware create(Vertx vertx, String name, JWKAccessibleAuthHandler authHandler, JsonObject middlewareConfig) {
        final String sessionScope = middlewareConfig.getString(MIDDLEWARE_PASS_AUTHORIZATION_SESSION_SCOPE);

        final Middleware passAuthorizationMiddleware = new PassAuthorizationMiddleware(vertx, name, sessionScope, authHandler);
        LOGGER.debug("Created '{}' middleware", MIDDLEWARE_PASS_AUTHORIZATION);
        return passAuthorizationMiddleware;
    }

}
