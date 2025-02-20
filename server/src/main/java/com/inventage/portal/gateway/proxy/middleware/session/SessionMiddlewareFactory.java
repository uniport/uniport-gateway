package com.inventage.portal.gateway.proxy.middleware.session;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link SessionMiddleware}.
 */
public class SessionMiddlewareFactory implements MiddlewareFactory {

    // schema
    // session
    public static final String SESSION = "session";
    public static final String SESSION_ID_MIN_LENGTH = "idMinimumLength";
    public static final String SESSION_IDLE_TIMEOUT_IN_MINUTES = "idleTimeoutInMinute";
    public static final String SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI = "uriWithoutSessionTimeoutReset";
    public static final String SESSION_NAG_HTTPS = "nagHttps";
    public static final String SESSION_LIFETIME_COOKIE = "lifetimeCookie";
    public static final String SESSION_LIFETIME_HEADER = "lifetimeHeader";

    // session cookie
    public static final String SESSION_COOKIE = "cookie";
    public static final String SESSION_COOKIE_HTTP_ONLY = "httpOnly";
    public static final String SESSION_COOKIE_NAME = "name";
    public static final String SESSION_COOKIE_SAME_SITE = "sameSite";
    public static final String SESSION_COOKIE_SECURE = "secure";

    // session store
    public static final String CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS = "clusteredSessionStoreRetryTimeoutInMiliseconds";

    // defaults
    // session
    public static final int DEFAULT_SESSION_ID_MINIMUM_LENGTH = 32;
    public static final int DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE = 15;
    public static final String DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI = null;
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
    public static final int DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS = 5 * 1000;

    private static final String[] COOKIE_SAME_SITE_POLICIES = new String[] {
        "NONE",
        "STRICT",
        "LAX"
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddlewareFactory.class);

    @Override
    public String provides() {
        return SESSION;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            // session
            .optionalProperty(SESSION_IDLE_TIMEOUT_IN_MINUTES, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE))
            .optionalProperty(SESSION_ID_MIN_LENGTH, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(DEFAULT_SESSION_ID_MINIMUM_LENGTH))
            .optionalProperty(SESSION_NAG_HTTPS, Schemas.booleanSchema()
                .defaultValue(DEFAULT_NAG_HTTPS))
            .optionalProperty(SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            // session lifetime
            .optionalProperty(SESSION_LIFETIME_COOKIE, Schemas.booleanSchema()
                .defaultValue(DEFAULT_SESSION_LIFETIME_COOKIE))
            .optionalProperty(SESSION_LIFETIME_HEADER, Schemas.booleanSchema()
                .defaultValue(DEFAULT_SESSION_LIFETIME_HEADER))
            // session cookie
            .optionalProperty(SESSION_COOKIE, Schemas.objectSchema()
                .optionalProperty(SESSION_COOKIE_NAME, Schemas.stringSchema()
                    .with(Keywords.minLength(1))
                    .defaultValue(DEFAULT_SESSION_COOKIE_NAME))
                .optionalProperty(SESSION_COOKIE_HTTP_ONLY, Schemas.booleanSchema()
                    .defaultValue(DEFAULT_SESSION_COOKIE_HTTP_ONLY))
                .optionalProperty(SESSION_COOKIE_SECURE, Schemas.booleanSchema()
                    .defaultValue(DEFAULT_SESSION_COOKIE_SECURE))
                .optionalProperty(SESSION_COOKIE_SAME_SITE, Schemas.enumSchema((Object[]) COOKIE_SAME_SITE_POLICIES)
                    .defaultValue(DEFAULT_SESSION_COOKIE_SAME_SITE))
                .allowAdditionalProperties(false))
            // session store
            .optionalProperty(CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        // session
        logDefaultIfNotConfigured(LOGGER, options, SESSION_IDLE_TIMEOUT_IN_MINUTES, DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE);
        logDefaultIfNotConfigured(LOGGER, options, SESSION_ID_MIN_LENGTH, DEFAULT_SESSION_ID_MINIMUM_LENGTH);
        logDefaultIfNotConfigured(LOGGER, options, SESSION_NAG_HTTPS, DEFAULT_NAG_HTTPS);
        logDefaultIfNotConfigured(LOGGER, options, SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI);

        // session lifetime
        logDefaultIfNotConfigured(LOGGER, options, SESSION_LIFETIME_COOKIE, DEFAULT_SESSION_LIFETIME_COOKIE);
        logDefaultIfNotConfigured(LOGGER, options, SESSION_LIFETIME_HEADER, DEFAULT_SESSION_LIFETIME_HEADER);

        // session cookie
        final JsonObject cookie = options.getJsonObject(SESSION_COOKIE);
        logDefaultIfNotConfigured(LOGGER, options, SESSION_COOKIE, String.format("Name=%s, HttpOnly=%s, SameSite=%s", DEFAULT_SESSION_COOKIE_NAME, DEFAULT_SESSION_COOKIE_HTTP_ONLY, DEFAULT_SESSION_COOKIE_SAME_SITE));

        if (cookie != null) {
            logDefaultIfNotConfigured(LOGGER, options, SESSION_COOKIE_NAME, DEFAULT_SESSION_COOKIE_NAME);
            logDefaultIfNotConfigured(LOGGER, options, SESSION_COOKIE_HTTP_ONLY, DEFAULT_SESSION_COOKIE_HTTP_ONLY);
            logDefaultIfNotConfigured(LOGGER, options, SESSION_COOKIE_SECURE, DEFAULT_SESSION_COOKIE_SECURE);
            logDefaultIfNotConfigured(LOGGER, options, SESSION_COOKIE_SAME_SITE, DEFAULT_SESSION_COOKIE_SAME_SITE);
        }

        // session store
        logDefaultIfNotConfigured(LOGGER, options, CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS, DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS);

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        // session
        final int sessionIdMinLength = middlewareConfig.getInteger(
            SESSION_ID_MIN_LENGTH,
            DEFAULT_SESSION_ID_MINIMUM_LENGTH);
        final int sessionIdleTimeoutInMinutes = middlewareConfig.getInteger(
            SESSION_IDLE_TIMEOUT_IN_MINUTES,
            DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE);
        final String pathWithoutSessionTimeoutReset = middlewareConfig.getString(
            SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI);
        final Boolean nagHttps = middlewareConfig.getBoolean(
            SESSION_NAG_HTTPS,
            DEFAULT_NAG_HTTPS);

        // session cookie
        final JsonObject sessionCookieConfig = middlewareConfig.getJsonObject(
            SESSION_COOKIE,
            new JsonObject());
        final String sessionCookieName = sessionCookieConfig.getString(
            SESSION_COOKIE_NAME,
            DEFAULT_SESSION_COOKIE_NAME);
        final boolean sessionCookieHttpOnly = sessionCookieConfig.getBoolean(
            SESSION_COOKIE_HTTP_ONLY,
            DEFAULT_SESSION_COOKIE_HTTP_ONLY);
        final boolean sessionCookieSecure = sessionCookieConfig.getBoolean(
            SESSION_COOKIE_SECURE,
            DEFAULT_SESSION_COOKIE_SECURE);
        final String sessionCookieSameSiteValue = sessionCookieConfig.getString(
            SESSION_COOKIE_SAME_SITE,
            DEFAULT_SESSION_COOKIE_SAME_SITE.toString());
        final CookieSameSite sessionCookieSameSite = CookieSameSite.valueOf(
            sessionCookieSameSiteValue.toUpperCase());

        // session lifetime
        final Boolean lifetimeHeader = middlewareConfig.getBoolean(
            SESSION_LIFETIME_HEADER,
            DEFAULT_SESSION_LIFETIME_HEADER);
        final String lifetimeHeaderName = DEFAULT_SESSION_LIFETIME_HEADER_NAME;

        final Boolean lifetimeCookie = middlewareConfig.getBoolean(
            SESSION_LIFETIME_COOKIE,
            DEFAULT_SESSION_LIFETIME_COOKIE);
        final String lifetimeCookieName = DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
        final String lifetimeCookiePath = DEFAULT_SESSION_LIFETIME_COOKIE_PATH;
        final boolean lifetimeCookieHttpOnly = DEFAULT_SESSION_LIFETIME_COOKIE_HTTP_ONLY;
        final boolean lifetimeCookieSecure = DEFAULT_SESSION_LIFETIME_COOKIE_SECURE;
        final CookieSameSite lifetimeCookieSameSite = DEFAULT_SESSION_LIFETIME_COOKIE_SAME_SITE;

        // session store
        final int clusteredSessionStoreRetryTimeoutMilliSeconds = middlewareConfig.getInteger(
            CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS,
            DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS);

        LOGGER.info("Created '{}' middleware successfully", SESSION);
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
                clusteredSessionStoreRetryTimeoutMilliSeconds));
    }
}
