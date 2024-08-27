package com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class PassAuthorizationMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassAuthorizationMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_PASS_AUTHORIZATION;
    }

    @Override
    protected Middleware create(Vertx vertx, String name, JWKAccessibleAuthHandler authHandler, JsonObject middlewareConfig) {
        final String sessionScope = middlewareConfig
            .getString(DynamicConfiguration.MIDDLEWARE_PASS_AUTHORIZATION_SESSION_SCOPE);

        final Middleware passAuthorizationMiddleware = new PassAuthorizationMiddleware(vertx, name, sessionScope, authHandler);
        LOGGER.debug("Created '{}' middleware", DynamicConfiguration.MIDDLEWARE_PASS_AUTHORIZATION);
        return passAuthorizationMiddleware;
    }

}
