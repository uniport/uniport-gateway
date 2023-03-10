package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseSessionCookieRemovalMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String sessionCookieName = middlewareConfig
                .getString(DynamicConfiguration.MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME);
        LOGGER.debug("Created '{}' middleware successfully",
                DynamicConfiguration.MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL);
        return Future.succeededFuture(new ResponseSessionCookieRemovalMiddleware(name, sessionCookieName));
    }
}
