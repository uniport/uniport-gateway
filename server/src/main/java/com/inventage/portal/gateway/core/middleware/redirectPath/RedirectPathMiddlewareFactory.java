package com.inventage.portal.gateway.core.middleware.redirectPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class RedirectPathMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(RedirectPathMiddlewareFactory.class);

    static final String MIDDLEWARE_REDIRECT_PATH_DESTINATION = "destination";

    @Override
    public String provides() {
        return "redirectPath";
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        return new RedirectPathMiddleware(
                middlewareConfig.getString(MIDDLEWARE_REDIRECT_PATH_DESTINATION));
    }
}
