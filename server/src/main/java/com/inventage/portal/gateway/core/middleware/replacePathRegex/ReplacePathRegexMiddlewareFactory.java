package com.inventage.portal.gateway.core.middleware.replacePathRegex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ReplacePathRegexMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(ReplacePathRegexMiddlewareFactory.class);

    static final String MIDDLEWARE_REPLACE_PATH_REGEX_REGEX = "regex";
    static final String MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT = "replacement";

    @Override
    public String provides() {
        return "redirectPathRegex";
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        return new ReplacePathRegexMiddleware(
                middlewareConfig.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX),
                middlewareConfig.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT));
    }
}
