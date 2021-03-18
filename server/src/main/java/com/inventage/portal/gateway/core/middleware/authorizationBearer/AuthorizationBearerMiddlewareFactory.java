package com.inventage.portal.gateway.core.middleware.authorizationBearer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class AuthorizationBearerMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddlewareFactory.class);

    @Override
    public String provides() {
        return "authorizationBearer";
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        return new AuthorizationBearerMiddleware();
    }

}
