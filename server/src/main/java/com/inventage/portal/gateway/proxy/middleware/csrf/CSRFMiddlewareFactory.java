package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSRFMiddlewareFactory implements MiddlewareFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSRFMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_CSRF;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String secret = UUID.randomUUID().toString();
        final JsonObject cookie = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_CSRF_COOKIE);
        final String cookieName = (cookie == null) ? null
            : cookie.getString(DynamicConfiguration.MIDDLEWARE_CSRF_COOKIE_NAME);
        final String cookiePath = (cookie == null) ? null
            : cookie.getString(DynamicConfiguration.MIDDLEWARE_CSRF_COOKIE_PATH);
        final Boolean cookieSecure = (cookie == null) ? null
            : cookie.getBoolean(DynamicConfiguration.MIDDLEWARE_CSRF_COOKIE_SECURE);
        final String headerName = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_CSRF_HEADER_NAME);
        final Long timeoutInMinute = middlewareConfig.getLong(DynamicConfiguration.MIDDLEWARE_CSRF_TIMEOUT_IN_MINUTES);
        final String origin = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_CSRF_ORIGIN);
        final Boolean nagHttps = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_CSRF_NAG_HTTPS);

        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_CSRF);
        return Future.succeededFuture(new CSRFMiddleware(vertx, name, secret, cookieName, cookiePath,
            cookieSecure, headerName, timeoutInMinute, origin, nagHttps));
    }
}
