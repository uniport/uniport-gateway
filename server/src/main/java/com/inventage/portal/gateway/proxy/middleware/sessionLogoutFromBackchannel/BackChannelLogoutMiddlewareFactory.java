package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.publickeysReconciler.JWTAuthPublicKeysReconcilerHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for BackChannelLogoutMiddleware.
 */
public class BackChannelLogoutMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    public static final String DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH = "/backchannellogout";

    private static final Logger LOGGER = LoggerFactory.getLogger(BackChannelLogoutMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BACK_CHANNEL_LOGOUT;
    }

    @Override
    protected Middleware create(final Vertx vertx, final String name, final JWTAuthPublicKeysReconcilerHandler authHandler, final JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_BACK_CHANNEL_LOGOUT);
        return new BackChannelLogoutMiddleware(vertx, name, DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH);
    }

    @Override
    public Future<Middleware> create(final Vertx vertx, final String name, final Router router, final JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_BACK_CHANNEL_LOGOUT);
        return Future.succeededFuture(
            new BackChannelLogoutMiddleware(vertx, name, DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH));
    }

}
