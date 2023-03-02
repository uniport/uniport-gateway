package com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.AuthenticationHandler;

public class PassAuthorizationMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassAuthorizationMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_PASS_AUTHORIZATION;
    }

    @Override
    protected Middleware create(String name, AuthenticationHandler authHandler, JsonObject middlewareConfig) {
        final String sessionScope = middlewareConfig
                .getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_TOKEN_SESSION_SCOPE);

        final Middleware passAuthorizationMiddleware = new PassAuthorizationMiddleware(name, sessionScope, authHandler);
        LOGGER.debug("Created '{}' middleware", DynamicConfiguration.MIDDLEWARE_PASS_AUTHORIZATION);
        return passAuthorizationMiddleware;
    }

}
