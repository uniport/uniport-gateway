package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link CSRFMiddleware}.
 */
public class CSRFMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String CSRF = "csrf";
    public static final String CSRF_COOKIE = "cookie";
    public static final String CSRF_COOKIE_NAME = "name";
    public static final String CSRF_COOKIE_PATH = "path";
    public static final String CSRF_COOKIE_SECURE = "secure";
    public static final String CSRF_HEADER_NAME = "headerName";
    public static final String CSRF_NAG_HTTPS = "nagHttps";
    public static final String CSRF_ORIGIN = "origin";
    public static final String CSRF_TIMEOUT_IN_MINUTES = "timeoutInMinute";

    // defaults
    public static final String DEFAULT_COOKIE_NAME = CSRFHandler.DEFAULT_COOKIE_NAME;
    public static final String DEFAULT_HEADER_NAME = CSRFHandler.DEFAULT_HEADER_NAME;
    public static final String DEFAULT_COOKIE_PATH = CSRFHandler.DEFAULT_COOKIE_PATH;
    public static final long DEFAULT_TIMEOUT_IN_MINUTES = 15;
    public static final boolean DEFAULT_COOKIE_SECURE = true;
    public static final boolean DEFAULT_NAG_HTTPS = true;
    public static final String DEFAULT_ORIGIN = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(CSRFMiddlewareFactory.class);

    @Override
    public String provides() {
        return CSRF;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(CSRF_COOKIE, Schemas.objectSchema()
                .optionalProperty(CSRF_COOKIE_NAME, Schemas.stringSchema()
                    .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
                .optionalProperty(CSRF_COOKIE_PATH, Schemas.stringSchema()
                    .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
                .optionalProperty(CSRF_COOKIE_SECURE, Schemas.booleanSchema())
                .allowAdditionalProperties(false))
            .optionalProperty(CSRF_HEADER_NAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .optionalProperty(CSRF_NAG_HTTPS, Schemas.booleanSchema())
            .optionalProperty(CSRF_ORIGIN, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .optionalProperty(CSRF_TIMEOUT_IN_MINUTES, Schemas.intSchema()
                .withKeyword(KEYWORD_INT_MIN, INT_MIN))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final Integer timeoutInMinutes = options.getInteger(CSRF_TIMEOUT_IN_MINUTES);
        if (timeoutInMinutes == null) {
            LOGGER.debug("csrf token timeout not specified. Use default value: %s");
        }
        final String origin = options.getString(CSRF_ORIGIN);
        if (origin != null && (origin.isEmpty() || origin.isBlank())) {
            return Future.failedFuture("if origin is defined it should not be empty or blank!");
        }
        final Boolean nagHttps = options.getBoolean(CSRF_NAG_HTTPS);
        if (nagHttps == null) {
            LOGGER.debug(String.format("NagHttps not specified. Use default value: %s", CSRFMiddlewareFactory.DEFAULT_NAG_HTTPS));
        }
        final String headerName = options.getString(CSRF_HEADER_NAME);
        if (headerName == null) {
            LOGGER.debug(String.format(": header name not specified. Use default value: %s", CSRFMiddlewareFactory.DEFAULT_HEADER_NAME));
        }
        final JsonObject cookie = options.getJsonObject(CSRF_COOKIE);
        if (cookie == null) {
            LOGGER.debug("%s: Cookie settings not specified. Use default setting");
        } else {
            final String cookieName = cookie.getString(CSRF_COOKIE_NAME);
            if (cookieName == null) {
                LOGGER.debug(String.format("No session cookie name specified to be removed. Use default value: %s", SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME));
            }
            final String cookiePath = cookie.getString(CSRF_COOKIE_PATH);
            if (cookiePath == null) {
                LOGGER.debug(String.format("No session cookie name specified to be removed. Use default value: %s", CSRFMiddlewareFactory.DEFAULT_COOKIE_NAME));
            }
            final Boolean cookieSecure = cookie.getBoolean(CSRF_COOKIE_SECURE);
            if (cookieSecure == null) {
                LOGGER.debug(String.format("No session cookie name specified to be removed. Use default value: %s", CSRFMiddlewareFactory.DEFAULT_COOKIE_SECURE));
            }
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String secret = UUID.randomUUID().toString();
        final JsonObject cookie = middlewareConfig.getJsonObject(CSRF_COOKIE, new JsonObject());
        final String cookieName = cookie.getString(CSRF_COOKIE_NAME, DEFAULT_COOKIE_NAME);
        final String cookiePath = cookie.getString(CSRF_COOKIE_PATH, DEFAULT_COOKIE_PATH);
        final boolean cookieSecure = cookie.getBoolean(CSRF_COOKIE_SECURE, DEFAULT_COOKIE_SECURE);
        final String headerName = middlewareConfig.getString(CSRF_HEADER_NAME, DEFAULT_HEADER_NAME);
        final long timeoutInMinute = middlewareConfig.getLong(CSRF_TIMEOUT_IN_MINUTES, DEFAULT_TIMEOUT_IN_MINUTES);
        final String origin = middlewareConfig.getString(CSRF_ORIGIN, DEFAULT_ORIGIN);
        final boolean nagHttps = middlewareConfig.getBoolean(CSRF_NAG_HTTPS, DEFAULT_NAG_HTTPS);

        LOGGER.info("Created '{}' middleware successfully", CSRF);
        return Future.succeededFuture(new CSRFMiddleware(vertx, name, secret, cookieName, cookiePath,
            cookieSecure, headerName, timeoutInMinute, origin, nagHttps));
    }
}
