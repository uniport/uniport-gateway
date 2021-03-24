package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class AuthorizationBearerMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthorizationBearerMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER;
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        return new AuthorizationBearerMiddleware(middlewareConfig
                .getString(DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE));
    }

}
