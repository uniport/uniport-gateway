package com.inventage.portal.gateway.core.middleware.redirectRegex;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class RedirectRegexMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX;
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        return new RedirectRegexMiddleware(
                middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REGEX),
                middlewareConfig
                        .getString(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT));
    }
}
