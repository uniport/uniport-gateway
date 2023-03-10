package com.inventage.portal.gateway.proxy.middleware.authorization.authorizationBearer;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationBearerMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully",
                DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER);
        return Future.succeededFuture(new AuthorizationBearerMiddleware(
                name,
                middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE)));
    }

}
