package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BearerOnlyMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BEARER_ONLY;
    }

    @Override
    protected Middleware create(String name, AuthenticationHandler authHandler, JsonObject middlewareConfig) {
        final String optionalStr = middlewareConfig.getString(
                DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_OPTIONAL,
                "false");
        final boolean optional = Boolean.parseBoolean(optionalStr);

        final Middleware bearerOnlyMiddleware = new BearerOnlyMiddleware(name, authHandler, optional);
        LOGGER.debug("Created '{}' middleware", DynamicConfiguration.MIDDLEWARE_BEARER_ONLY);
        return bearerOnlyMiddleware;
    }
}
