package com.inventage.portal.gateway.proxy.middleware.csrf;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.json.schema.common.dsl.Keywords;
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
    public static final String DEFAULT_COOKIE_PATH = CSRFHandler.DEFAULT_COOKIE_PATH;
    public static final boolean DEFAULT_COOKIE_SECURE = true;
    public static final String DEFAULT_HEADER_NAME = CSRFHandler.DEFAULT_HEADER_NAME;
    public static final boolean DEFAULT_NAG_HTTPS = true;
    public static final String DEFAULT_ORIGIN = null;
    public static final long DEFAULT_TIMEOUT_IN_MINUTES = 15;

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
                    .with(Keywords.minLength(1))
                    .defaultValue(DEFAULT_COOKIE_NAME))
                .optionalProperty(CSRF_COOKIE_PATH, Schemas.stringSchema()
                    .with(Keywords.minLength(1))
                    .defaultValue(DEFAULT_COOKIE_PATH))
                .optionalProperty(CSRF_COOKIE_SECURE, Schemas.booleanSchema()
                    .defaultValue(DEFAULT_COOKIE_SECURE))
                .allowAdditionalProperties(false))
            .optionalProperty(CSRF_HEADER_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_HEADER_NAME))
            .optionalProperty(CSRF_NAG_HTTPS, Schemas.booleanSchema()
                .defaultValue(DEFAULT_NAG_HTTPS))
            .optionalProperty(CSRF_ORIGIN, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(CSRF_TIMEOUT_IN_MINUTES, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(DEFAULT_TIMEOUT_IN_MINUTES))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonObject cookie = options.getJsonObject(CSRF_COOKIE);
        logDefaultIfNotConfigured(LOGGER, options, CSRF_COOKIE, String.format("Name=%s, Path=%s, Secure=%s", DEFAULT_COOKIE_NAME, DEFAULT_COOKIE_PATH, DEFAULT_COOKIE_SECURE));

        if (cookie != null) {
            logDefaultIfNotConfigured(LOGGER, options, CSRF_COOKIE_NAME, DEFAULT_COOKIE_NAME);
            logDefaultIfNotConfigured(LOGGER, options, CSRF_COOKIE_PATH, DEFAULT_COOKIE_PATH);
            logDefaultIfNotConfigured(LOGGER, options, CSRF_COOKIE_SECURE, DEFAULT_COOKIE_SECURE);
        }

        logDefaultIfNotConfigured(LOGGER, options, CSRF_HEADER_NAME, DEFAULT_HEADER_NAME);
        logDefaultIfNotConfigured(LOGGER, options, CSRF_TIMEOUT_IN_MINUTES, DEFAULT_TIMEOUT_IN_MINUTES);
        logDefaultIfNotConfigured(LOGGER, options, CSRF_ORIGIN, DEFAULT_ORIGIN);
        logDefaultIfNotConfigured(LOGGER, options, CSRF_NAG_HTTPS, DEFAULT_NAG_HTTPS);

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String secret = UUID.randomUUID().toString();
        final JsonObject cookie = middlewareConfig.getJsonObject(CSRF_COOKIE, JsonObject.of());
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
