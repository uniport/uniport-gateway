package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class PreventForeignInitiatedAuthMiddlewareFactory implements MiddlewareFactory {

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_PREVENT_FOREIGN_INITIATED_AUTHENTICATION;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' of type '{}' middleware successfully", name,
            DynamicConfiguration.MIDDLEWARE_PREVENT_FOREIGN_INITIATED_AUTHENTICATION);
        final String redirect = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT);
        return Future.succeededFuture(new PreventForeignInitiatedAuthMiddleware(name, redirect));
    }
}
