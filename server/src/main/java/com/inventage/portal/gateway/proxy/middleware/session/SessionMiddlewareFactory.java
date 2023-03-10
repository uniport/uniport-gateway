package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_SESSION;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final JsonObject cookie = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE);
        final String cookieName = (cookie == null) ? null
                : cookie.getString(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_NAME);
        final Boolean cookieHttpOnly = (cookie == null) ? null
                : cookie.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY);
        final Boolean cookieSecure = (cookie == null) ? null
                : cookie.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SECURE);
        final String cookieSameSite = (cookie == null) ? null
                : cookie.getString(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SAME_SITE);
        final Long sessionIdleTimeoutInMinutes = middlewareConfig
                .getLong(DynamicConfiguration.MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES);
        final Integer sessionIdMinLength = middlewareConfig
                .getInteger(DynamicConfiguration.MIDDLEWARE_SESSION_ID_MIN_LENGTH);
        final Boolean nagHttps = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_NAG_HTTPS);
        final Boolean lifetimeHeader = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_LIFETIME_HEADER);
        final Boolean lifetimeCookie = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_LIFETIME_COOKIE);

        LOGGER.info("Created '{}' middleware successfully",
                DynamicConfiguration.MIDDLEWARE_SESSION);
        return Future.succeededFuture(new SessionMiddleware(vertx, name, sessionIdleTimeoutInMinutes, lifetimeHeader, lifetimeCookie, cookieName,
                cookieHttpOnly, cookieSecure, cookieSameSite, sessionIdMinLength, nagHttps));
    }
}
