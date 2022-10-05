package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class ResponseSessionCookieFactory implements MiddlewareFactory {

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_RESPONSE_SESSION_COOKIE;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        String sessionCookieName = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_RESPONSE_SESSION_COOKIE_NAME);
        LOGGER.debug("Created '{}' middleware successfully",
                DynamicConfiguration.MIDDLEWARE_RESPONSE_SESSION_COOKIE);
        return Future.succeededFuture(new ResponseSessionCookieMiddleware(sessionCookieName));
    }
}
