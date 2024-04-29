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

/**
 */
public class SessionMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddlewareFactory.class);

    // sesion
    public static final int DEFAULT_SESSION_ID_MINIMUM_LENGTH = 32;
    public static final int DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE = 15;
    public static final boolean DEFAULT_NAG_HTTPS = true;

    // session cookie
    public static final String DEFAULT_SESSION_COOKIE_NAME = "uniport.session";
    public static final boolean DEFAULT_SESSION_COOKIE_HTTP_ONLY = true;
    public static final boolean DEFAULT_SESSION_COOKIE_SECURE = false;
    public static final CookieSameSite DEFAULT_SESSION_COOKIE_SAME_SITE = CookieSameSite.STRICT;

    // session lifetime
    public static final boolean DEFAULT_SESSION_LIFETIME_HEADER = false;
    public static final String DEFAULT_SESSION_LIFETIME_HEADER_NAME = "x-uniport-session-lifetime";

    public static final boolean DEFAULT_SESSION_LIFETIME_COOKIE = false;
    public static final String DEFAULT_SESSION_LIFETIME_COOKIE_NAME = "uniport.session-lifetime";
    public static final String DEFAULT_SESSION_LIFETIME_COOKIE_PATH = "/";
    public static final boolean DEFAULT_SESSION_LIFETIME_COOKIE_HTTP_ONLY = false; // false := cookie must be accessible by client side scripts
    public static final boolean DEFAULT_SESSION_LIFETIME_COOKIE_SECURE = false;
    public static final CookieSameSite DEFAULT_SESSION_LIFETIME_COOKIE_SAME_SITE = CookieSameSite.STRICT; // prevent warnings in Firefox console

    // session store
    public static final int DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILISECONDS = 5 * 1000;

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_SESSION;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        // session
        final int sessionIdMinLength = middlewareConfig.getInteger(
            DynamicConfiguration.MIDDLEWARE_SESSION_ID_MIN_LENGTH,
            DEFAULT_SESSION_ID_MINIMUM_LENGTH);
        final int sessionIdleTimeoutInMinutes = middlewareConfig.getInteger(
            DynamicConfiguration.MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES,
            DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE);
        final String pathWithoutSessionTimeoutReset = middlewareConfig.getString(
            DynamicConfiguration.MIDDLEWARE_SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI);
        final Boolean nagHttps = middlewareConfig.getBoolean(
            DynamicConfiguration.MIDDLEWARE_SESSION_NAG_HTTPS,
            DEFAULT_NAG_HTTPS);

        // session cookie
        final JsonObject sessionCookieConfig = middlewareConfig.getJsonObject(
            DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE,
            new JsonObject());
        final String sessionCookieName = sessionCookieConfig.getString(
            DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_NAME,
            DEFAULT_SESSION_COOKIE_NAME);
        final boolean sessionCookieHttpOnly = sessionCookieConfig.getBoolean(
            DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY,
            DEFAULT_SESSION_COOKIE_HTTP_ONLY);
        final boolean sessionCookieSecure = sessionCookieConfig.getBoolean(
            DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SECURE,
            DEFAULT_SESSION_COOKIE_SECURE);
        final String sessionCookieSameSiteValue = sessionCookieConfig.getString(
            DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SAME_SITE,
            DEFAULT_SESSION_COOKIE_SAME_SITE.toString());
        final CookieSameSite sessionCookieSameSite = CookieSameSite.valueOf(
            sessionCookieSameSiteValue.toUpperCase());

        // session lifetime
        final Boolean lifetimeHeader = middlewareConfig.getBoolean(
            DynamicConfiguration.MIDDLEWARE_SESSION_LIFETIME_HEADER,
            DEFAULT_SESSION_LIFETIME_HEADER);
        final String lifetimeHeaderName = DEFAULT_SESSION_LIFETIME_HEADER_NAME;

        final Boolean lifetimeCookie = middlewareConfig.getBoolean(
            DynamicConfiguration.MIDDLEWARE_SESSION_LIFETIME_COOKIE,
            DEFAULT_SESSION_LIFETIME_COOKIE);
        final String lifetimeCookieName = DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
        final String lifetimeCookiePath = DEFAULT_SESSION_LIFETIME_COOKIE_PATH;
        final boolean lifetimeCookieHttpOnly = DEFAULT_SESSION_LIFETIME_COOKIE_HTTP_ONLY;
        final boolean lifetimeCookieSecure = DEFAULT_SESSION_LIFETIME_COOKIE_SECURE;
        final CookieSameSite lifetimeCookieSameSite = DEFAULT_SESSION_LIFETIME_COOKIE_SAME_SITE;

        // session store
        final int clusteredSessionStoreRetryTimeoutMiliSeconds = middlewareConfig.getInteger(
            DynamicConfiguration.MIDDLEWARE_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS,
            DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILISECONDS);

        LOGGER.info("Created '{}' middleware successfully",
            DynamicConfiguration.MIDDLEWARE_SESSION);
        return Future.succeededFuture(
            new SessionMiddleware(
                vertx,
                name,
                sessionIdMinLength,
                sessionIdleTimeoutInMinutes,
                pathWithoutSessionTimeoutReset,
                nagHttps,
                sessionCookieName,
                sessionCookieHttpOnly,
                sessionCookieSecure,
                sessionCookieSameSite,
                lifetimeHeader,
                lifetimeHeaderName,
                lifetimeCookie,
                lifetimeCookieName,
                lifetimeCookiePath,
                lifetimeCookieHttpOnly,
                lifetimeCookieSecure,
                lifetimeCookieSameSite,
                clusteredSessionStoreRetryTimeoutMiliSeconds));
    }
}
