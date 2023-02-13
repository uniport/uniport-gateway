package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import com.inventage.portal.gateway.proxy.middleware.withAuthHandler.MiddlewareWithAuthHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.AuthenticationHandler;

public class BearerOnlyMiddlewareFactory extends MiddlewareWithAuthHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BEARER_ONLY;
    }

    @Override
    protected Middleware createMiddleware(AuthenticationHandler authHandler, JsonObject middlewareConfig) {
        final String optionalStr = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_OPTIONAL,
                "false");
        final boolean optional = Boolean.parseBoolean(optionalStr);

        final Middleware bearerOnlyMiddleware = new BearerOnlyMiddleware(authHandler, optional);
        LOGGER.debug("Created '{}' middleware", DynamicConfiguration.MIDDLEWARE_BEARER_ONLY);
        return  bearerOnlyMiddleware;
    }
}
