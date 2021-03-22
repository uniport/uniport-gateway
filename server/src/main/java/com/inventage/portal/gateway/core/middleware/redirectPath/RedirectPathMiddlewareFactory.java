package com.inventage.portal.gateway.core.middleware.redirectPath;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class RedirectPathMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(RedirectPathMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_REDIRECT_PATH;
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        return new RedirectPathMiddleware(
                middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_REDIRECT_PATH_DESTINATION));
    }
}
