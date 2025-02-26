package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link BackChannelLogoutMiddleware}.
 */
public class BackChannelLogoutMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    // schema
    public static final String BACK_CHANNEL_LOGOUT = "backChannelLogout";

    private static final Logger LOGGER = LoggerFactory.getLogger(BackChannelLogoutMiddlewareFactory.class);

    @Override
    public String provides() {
        return BACK_CHANNEL_LOGOUT;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return super.optionsSchema();
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return super.validate(options);
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return BackChannelLogoutMiddlewareOptions.class;
    }

    @Override
    protected Middleware create(final Vertx vertx, final String name, final JWKAccessibleAuthHandler authHandler, final JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", BACK_CHANNEL_LOGOUT);
        return new BackChannelLogoutMiddleware(vertx, name, authHandler);
    }
}
