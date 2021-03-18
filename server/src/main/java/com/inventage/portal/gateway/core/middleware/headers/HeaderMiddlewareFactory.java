package com.inventage.portal.gateway.core.middleware.headers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class HeaderMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(HeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return "headers";
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        return new HeaderMiddleware();
    }

}


