package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class ReplacedSessionCookieDetectionMiddlewareFactory implements MiddlewareFactory {

        private static final Logger LOGGER = LoggerFactory
                        .getLogger(ReplacedSessionCookieDetectionMiddlewareFactory.class);

        @Override
        public String provides() {
                return DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION;
        }

        @Override
        public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
                final String cookieName = middlewareConfig
                                .getString(DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME);
                final Integer waitTimeRetryInMs = middlewareConfig
                                .getInteger(DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS);

                LOGGER.debug("Created '{}' middleware successfully",
                                DynamicConfiguration.MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION);
                return Future
                                .succeededFuture(new ReplacedSessionCookieDetectionMiddleware(name, cookieName,
                                                waitTimeRetryInMs));
        }
}
