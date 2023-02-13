package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class AuthorizationBearerMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully",
                DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER);
        return Future.succeededFuture(new AuthorizationBearerMiddleware(
                middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_TOKEN_SESSION_SCOPE)));
    }

}
