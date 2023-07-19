package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddlewareFactory.class);
    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";
    public static final String DEFAULT_SESSION_COOKIE_NAME = "uniport.session";
    public static final String DEFAULT_SESSION_LIFETIME_COOKIE_NAME = "uniport.session-lifetime";
    public static final String DEFAULT_SESSION_LIFETIME_HEADER_NAME = "x-uniport-session-lifetime";
    public static final boolean DEFAULT_COOKIE_HTTP_ONLY = true;
    public static final boolean DEFAULT_COOKIE_SECURE = false;
    public static final CookieSameSite DEFAULT_COOKIE_SAME_SITE = CookieSameSite.STRICT;
    public static final int DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE = 15;
    public static final int DEFAULT_SESSION_ID_MINIMUM_LENGTH = 32;
    public static final boolean DEFAULT_NAG_HTTPS = true;
    public static final boolean DEFAULT_SESSION_LIFETIME_HEADER = false;
    public static final boolean DEFAULT_SESSION_LIFETIME_COOKIE = false;

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_SESSION;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final JsonObject cookie = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE);
        final String cookieName = cookie.getString(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_NAME, DEFAULT_SESSION_COOKIE_NAME);
        final boolean cookieHttpOnly = cookie.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY, DEFAULT_COOKIE_HTTP_ONLY);
        final boolean cookieSecure = cookie.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SECURE, DEFAULT_COOKIE_SECURE);
        final String cookieSameSiteValue = cookie.getString(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SAME_SITE, String.valueOf(DEFAULT_COOKIE_SAME_SITE));
        final CookieSameSite cookieSameSite = CookieSameSite.valueOf(cookieSameSiteValue);
        final int sessionIdleTimeoutInMinutes = middlewareConfig
            .getInteger(DynamicConfiguration.MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES, DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE);
        final int sessionIdMinLength = middlewareConfig
            .getInteger(DynamicConfiguration.MIDDLEWARE_SESSION_ID_MIN_LENGTH, DEFAULT_SESSION_ID_MINIMUM_LENGTH);
        final Boolean nagHttps = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_NAG_HTTPS, DEFAULT_NAG_HTTPS);
        final Boolean lifetimeHeader = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_LIFETIME_HEADER);
        final Boolean lifetimeCookie = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_SESSION_LIFETIME_COOKIE);
        final String pathWithoutSessionTimeoutReset = middlewareConfig
            .getString(DynamicConfiguration.MIDDLEWARE_SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI);

        LOGGER.info("Created '{}' middleware successfully",
            DynamicConfiguration.MIDDLEWARE_SESSION);
        return Future.succeededFuture(new SessionMiddleware(vertx, name, sessionIdleTimeoutInMinutes, lifetimeHeader, lifetimeCookie, cookieName,
            cookieHttpOnly, cookieSecure, cookieSameSite, sessionIdMinLength, nagHttps, pathWithoutSessionTimeoutReset));
    }
}
