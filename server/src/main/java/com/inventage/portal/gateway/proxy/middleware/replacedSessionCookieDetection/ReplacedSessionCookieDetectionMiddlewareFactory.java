package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ReplacedSessionCookieDetectionMiddlewareFactory implements MiddlewareFactory {

    public static final String DEFAULT_DETECTION_COOKIE_NAME = "uniport.state";
    public static final String DEFAULT_SESSION_COOKIE_NAME = SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    public static final int DEFAULT_WAIT_BEFORE_RETRY_MS = 50;
    public static final int DEFAULT_MAX_REDIRECT_RETRIES = 5;

    private static final Logger LOGGER = LoggerFactory
        .getLogger(ReplacedSessionCookieDetectionMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String detectionCookieName = middlewareConfig.getString(
            DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME,
            DEFAULT_DETECTION_COOKIE_NAME);
        final String sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;
        final Integer waitTimeRetryInMs = middlewareConfig.getInteger(
            DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS,
            DEFAULT_WAIT_BEFORE_RETRY_MS);
        final Integer maxRedirectRetries = middlewareConfig.getInteger(
            DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES,
            DEFAULT_MAX_REDIRECT_RETRIES);

        LOGGER.debug("Created '{}' middleware successfully",
            DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION);
        return Future.succeededFuture(
            new ReplacedSessionCookieDetectionMiddleware(
                name,
                detectionCookieName,
                sessionCookieName,
                waitTimeRetryInMs,
                maxRedirectRetries));
    }
}
