package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieUtil;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
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
    public static final String TYPE = "csrf";
    public static final String COOKIE = "cookie";
    public static final String COOKIE_NAME = "name";
    public static final String COOKIE_PATH = "path";
    public static final String COOKIE_SECURE = "secure";
    public static final String HEADER_NAME = "headerName";
    public static final String NAG_HTTPS = "nagHttps";
    public static final String ORIGIN = "origin";
    public static final String TIMEOUT_IN_MINUTES = "timeoutInMinute";

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
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(COOKIE, Schemas.objectSchema()
                .optionalProperty(COOKIE_NAME, Schemas.stringSchema()
                    .with(Keywords.minLength(1))
                    .defaultValue(DEFAULT_COOKIE_NAME))
                .optionalProperty(COOKIE_PATH, Schemas.stringSchema()
                    .with(Keywords.minLength(1))
                    .defaultValue(DEFAULT_COOKIE_PATH))
                .optionalProperty(COOKIE_SECURE, Schemas.booleanSchema()
                    .defaultValue(DEFAULT_COOKIE_SECURE))
                .allowAdditionalProperties(false))
            .optionalProperty(HEADER_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_HEADER_NAME))
            .optionalProperty(NAG_HTTPS, Schemas.booleanSchema()
                .defaultValue(DEFAULT_NAG_HTTPS))
            .optionalProperty(ORIGIN, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(TIMEOUT_IN_MINUTES, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(DEFAULT_TIMEOUT_IN_MINUTES))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        if (options.containsKey(COOKIE) &&
            options.getJsonObject(COOKIE).containsKey(COOKIE_NAME) &&
            !CookieUtil.isValidCookieName(LOGGER, options.getJsonObject(COOKIE).getString(COOKIE_NAME))) {
            return Future.failedFuture("cookie name is invalid");
        }
        return Future.succeededFuture();
    }

    @Override
    public Class<CSRFMiddlewareOptions> modelType() {
        return CSRFMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final CSRFMiddlewareOptions options = castOptions(config, modelType());
        final String secret = UUID.randomUUID().toString();

        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new CSRFMiddleware(vertx,
                name,
                secret,
                options.getCookie().getName(),
                options.getCookie().getPath(),
                options.getCookie().isSecure(),
                options.getHeaderName(),
                options.getTimeoutMinutes(),
                options.getOrigin(),
                options.nagHTTPs()));
    }
}
