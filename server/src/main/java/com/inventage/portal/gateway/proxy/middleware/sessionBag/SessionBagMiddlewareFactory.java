package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionBagMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddlewareFactory.class);
    private static final String DEFAULT_SESSION_COOKIE_NAME = SessionMiddleware.SESSION_COOKIE_NAME_DEFAULT;

    private static final String MIDDLEWARE_SESSION_BAG = "sessionBag";

    @Override
    public String provides() {
        return MIDDLEWARE_SESSION_BAG;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareOptions) {
        return this.create(vertx, name, middlewareOptions);
    }

    public Future<Middleware> create(Vertx vertx, String name, JsonObject middlewareOptions) {
        final JsonArray whitelistedCookies = middlewareOptions
            .getJsonArray(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES);

        String sessionCookieName = middlewareOptions.getString(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_COOKIE_NAME);
        if (sessionCookieName == null) {
            sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;
        }
        LOGGER.info("Created '{}' middleware successfully", MIDDLEWARE_SESSION_BAG);
        return Future.succeededFuture(new SessionBagMiddleware(name, whitelistedCookies, sessionCookieName));
    }

}
