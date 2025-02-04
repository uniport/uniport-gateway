package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link SessionMiddleware}.
 */
public class SessionMiddlewareFactory implements MiddlewareFactory {

    // schema
    // session
    public static final String SESSION = "session";
    public static final String SESSION_IDLE_TIMEOUT_IN_MINUTES = "idleTimeoutInMinute";
    public static final String SESSION_ID_MIN_LENGTH = "idMinimumLength";
    public static final String SESSION_LIFETIME_COOKIE = "lifetimeCookie";
    public static final String SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI = "uriWithoutSessionTimeoutReset";
    public static final String SESSION_LIFETIME_HEADER = "lifetimeHeader";
    public static final String SESSION_NAG_HTTPS = "nagHttps";

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

    private static final List<String> COOKIE_SAME_SITE_POLICIES = List.of("NONE", "STRICT", "LAX");

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
                .withKeyword(KEYWORD_INT_MIN, INT_MIN))
            .optionalProperty(SESSION_ID_MIN_LENGTH, Schemas.intSchema()
                .withKeyword(KEYWORD_INT_MIN, INT_MIN))
            // session lifetime
            .optionalProperty(SESSION_LIFETIME_COOKIE, Schemas.booleanSchema())
            .optionalProperty(SESSION_LIFETIME_HEADER, Schemas.booleanSchema())
            .optionalProperty(SESSION_NAG_HTTPS, Schemas.booleanSchema())
            .optionalProperty(SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            // session cookie
            .optionalProperty(SESSION_COOKIE, Schemas.objectSchema()
                .optionalProperty(SESSION_COOKIE_NAME, Schemas.stringSchema()
                    .withKeyword(KEYWORD_STRING_MIN_LENGTH, INT_MIN))
                .optionalProperty(SESSION_COOKIE_HTTP_ONLY, Schemas.booleanSchema())
                .optionalProperty(SESSION_COOKIE_SECURE, Schemas.booleanSchema())
                .optionalProperty(SESSION_COOKIE_SAME_SITE, Schemas.stringSchema()
                    .withKeyword(KEYWORD_ENUM, JsonArray.of(COOKIE_SAME_SITE_POLICIES.toArray())))
                .allowAdditionalProperties(false))
            // session store
            .optionalProperty(CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS, Schemas.intSchema()
                .withKeyword(KEYWORD_INT_MIN, INT_MIN))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final Integer sessionIdleTimeoutInMinutes = options.getInteger(SESSION_IDLE_TIMEOUT_IN_MINUTES);
        if (sessionIdleTimeoutInMinutes == null) {
            LOGGER.debug(String.format("Session idle timeout not specified. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE));
        }
        final Integer sessionIdMinLength = options.getInteger(SESSION_ID_MIN_LENGTH);
        if (sessionIdMinLength == null) {
            LOGGER.debug(String.format("Minimum session id length not specified. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_ID_MINIMUM_LENGTH));
        }
        final Boolean nagHttps = options.getBoolean(SESSION_NAG_HTTPS);
        if (nagHttps == null) {
            LOGGER.debug(String.format("NagHttps not specified. Use default value: %s", SessionMiddlewareFactory.DEFAULT_NAG_HTTPS));
        }
        final Boolean lifetimeHeader = options.getBoolean(SESSION_LIFETIME_HEADER);
        if (lifetimeHeader == null) {
            LOGGER.debug(String.format("LifetimeHeader not specified. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER));
        }
        final Boolean lifetimeCookie = options.getBoolean(SESSION_LIFETIME_COOKIE);
        if (lifetimeCookie == null) {
            LOGGER.debug(String.format("LifetimeCookie not specified. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE));
        }
        final String uriWithoutSessionTimeoutReset = options.getString(SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI);
        if (uriWithoutSessionTimeoutReset == null) {
            LOGGER.debug("URI without session timeout reset not specified.");
        }

        final JsonObject cookie = options.getJsonObject(SESSION_COOKIE);
        if (cookie == null) {
            LOGGER.debug("Cookie settings not specified. Use default setting");
        } else {
            final String cookieName = cookie.getString(SESSION_COOKIE_NAME);
            if (cookieName == null) {
                LOGGER.debug(String.format("No session cookie name specified to be removed. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME));
            }
            final Boolean cookieHttpOnly = cookie.getBoolean(SESSION_COOKIE_HTTP_ONLY);
            if (cookieHttpOnly == null) {
                LOGGER.debug(String.format("Cookie HttpOnly not specified. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_HTTP_ONLY));
            }
            final String cookieSameSite = cookie.getString(SESSION_COOKIE_SAME_SITE);
            if (cookieSameSite == null) {
                LOGGER.debug(String.format("Cookie SameSite not specified. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SAME_SITE));
            }
        }

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
        final int clusteredSessionStoreRetryTimeoutMiliSeconds = middlewareConfig.getInteger(
            CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS,
            DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILISECONDS);

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
                clusteredSessionStoreRetryTimeoutMiliSeconds));
    }
}
