package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class RedirectRegexMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddlewareFactory.class);

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
