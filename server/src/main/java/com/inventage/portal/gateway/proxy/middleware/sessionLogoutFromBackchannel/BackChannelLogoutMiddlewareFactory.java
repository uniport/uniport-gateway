package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for BackChannelLogoutMiddleware.
 */
public class BackChannelLogoutMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackChannelLogoutMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BACK_CHANNEL_LOGOUT;
    }

    @Override
    protected Middleware create(final Vertx vertx, final String name, final JWKAccessibleAuthHandler authHandler, final JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_BACK_CHANNEL_LOGOUT);
        return new BackChannelLogoutMiddleware(vertx, name, authHandler);
    }
}
