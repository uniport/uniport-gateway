package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class SessionFactory implements MiddlewareFactory {

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_SESSION;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        JsonObject cookie = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE);
        String cookieName = cookie.getString(DynamicConfiguration.MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME);
        boolean cookieHttpOnly = cookie.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY);
        boolean cookieSecure = cookie.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SECURE);
        String cookieSameSite = cookie.getString(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SAME_SITE);
        long sessionIdleTimeoutInMinutes = middlewareConfig.getInteger(DynamicConfiguration.MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES);
        int sessionIdMinLength = middlewareConfig.getInteger(DynamicConfiguration.MIDDLEWARE_SESSION_ID_MIN_LENGTH);
        boolean nagHttps = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_NAG_HTTPS);

        LOGGER.info("Created '{}' middleware successfully",
                DynamicConfiguration.MIDDLEWARE_SESSION);
        return Future.succeededFuture(new SessionMiddleware(vertx, sessionIdleTimeoutInMinutes, cookieName,
                cookieHttpOnly, cookieSecure, cookieSameSite, sessionIdMinLength, nagHttps));
    }
}
