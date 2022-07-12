package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class SessionBagMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddlewareFactory.class);

    private static final String MIDDLEWARE_SESSION_BAG = "sessionBag";

    @Override
    public String provides() {
        return MIDDLEWARE_SESSION_BAG;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareOptions) {
        return this.create(vertx, middlewareOptions);
    }

    public Future<Middleware> create(Vertx vertx, JsonObject middlewareOptions) {
        JsonArray whithelistedCookies = middlewareOptions
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIES);
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_SESSION_BAG);
        return Future.succeededFuture(new SessionBagMiddleware(whithelistedCookies));
    }

}
