package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LanguageCookieMiddlewareFactory implements MiddlewareFactory {

    public static final String DEFAULT_LANGUAGE_COOKIE_NAME = "uniport.language";
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_LANGUAGE_COOKIE;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully",
            DynamicConfiguration.MIDDLEWARE_LANGUAGE_COOKIE);
        final String languageCookieName = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_LANGUAGE_COOKIE_NAME, DEFAULT_LANGUAGE_COOKIE_NAME);
        return Future.succeededFuture(new LanguageCookieMiddleware(name, languageCookieName));
    }
}
