package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ReplacedSessionCookieDetectionMiddleware}.
 */
public class ReplacedSessionCookieDetectionMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String REPLACED_SESSION_COOKIE_DETECTION = "replacedSessionCookieDetection";
    public static final String REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME = "name";
    public static final String REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS = "waitTimeInMillisecond";
    public static final String REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES = "maxRedirectRetries";

    // defaults
    public static final String DEFAULT_DETECTION_COOKIE_NAME = "uniport.state";
    public static final String DEFAULT_SESSION_COOKIE_NAME = SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    public static final int DEFAULT_WAIT_BEFORE_RETRY_MS = 50;
    public static final int DEFAULT_MAX_REDIRECT_RETRIES = 5;

    private static final Logger LOGGER = LoggerFactory
        .getLogger(ReplacedSessionCookieDetectionMiddlewareFactory.class);

    @Override
    public String provides() {
        return REPLACED_SESSION_COOKIE_DETECTION;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, ONE))
            .optionalProperty(REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS, Schemas.intSchema()
                .withKeyword(KEYWORD_INT_MIN, ZERO))
            .optionalProperty(REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES, Schemas.intSchema()
                .withKeyword(KEYWORD_INT_MIN, ZERO))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME, DEFAULT_DETECTION_COOKIE_NAME);
        logDefaultIfNotConfigured(LOGGER, options, REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS, DEFAULT_WAIT_BEFORE_RETRY_MS);
        logDefaultIfNotConfigured(LOGGER, options, REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES, DEFAULT_MAX_REDIRECT_RETRIES);

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String sessionCookieName = DEFAULT_SESSION_COOKIE_NAME; // not configurable as it must fit

        final String detectionCookieName = middlewareConfig.getString(
            REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME,
            DEFAULT_DETECTION_COOKIE_NAME);
        final Integer waitTimeRetryInMs = middlewareConfig.getInteger(
            REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS,
            DEFAULT_WAIT_BEFORE_RETRY_MS);
        final Integer maxRedirectRetries = middlewareConfig.getInteger(
            REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES,
            DEFAULT_MAX_REDIRECT_RETRIES);

        LOGGER.debug("Created '{}' middleware successfully", REPLACED_SESSION_COOKIE_DETECTION);
        return Future.succeededFuture(
            new ReplacedSessionCookieDetectionMiddleware(
                name,
                detectionCookieName,
                sessionCookieName,
                waitTimeRetryInMs,
                maxRedirectRetries));
    }
}
