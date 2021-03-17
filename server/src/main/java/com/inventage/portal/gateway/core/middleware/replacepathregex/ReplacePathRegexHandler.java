package com.inventage.portal.gateway.core.middleware.replacepathregex;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface ReplacePathRegexHandler extends Handler<RoutingContext> {

    static final String MIDDLEWARE_REPLACE_PATH_REGEX_REGEX = "regex";
    static final String MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT = "replacement";

    static ReplacePathRegexHandler create(JsonObject middlewareConfig) {
        return new ReplacePathRegexHandlerImpl(
                middlewareConfig.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX),
                middlewareConfig.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT));
    }
}
