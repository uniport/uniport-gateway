package com.inventage.portal.gateway.proxy.middleware.passAuthorization;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.withAuthHandler.MiddlewareWithAuthHandlerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PassAuthorizationMiddlewareFactory extends MiddlewareWithAuthHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassAuthorizationMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_PASS_AUTHORIZATION;
    }

    @Override
    protected Middleware createMiddleware(AuthenticationHandler authHandler, JsonObject middlewareConfig) {
        final String sessionScope = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_TOKEN_SESSION_SCOPE);

        final Middleware passAuthorizationMiddleware = new PassAuthorizationMiddleware(sessionScope, authHandler);
        LOGGER.debug("Created '{}' middleware", DynamicConfiguration.MIDDLEWARE_PASS_AUTHORIZATION);
        return  passAuthorizationMiddleware;
    }

}
