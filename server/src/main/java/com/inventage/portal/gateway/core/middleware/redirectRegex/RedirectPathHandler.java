package com.inventage.portal.gateway.core.middleware.redirectRegex;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface RedirectPathHandler extends Handler<RoutingContext> {

    static final String MIDDLEWARE_REDIRECT_PATH_DESTINATION = "destination";

    static RedirectPathHandler create(JsonObject middlewareConfig) {
        return new RedirectPathHandlerImpl(
                middlewareConfig.getString(MIDDLEWARE_REDIRECT_PATH_DESTINATION));
    }
}
