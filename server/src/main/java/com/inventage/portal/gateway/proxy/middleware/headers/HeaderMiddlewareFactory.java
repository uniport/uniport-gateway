package com.inventage.portal.gateway.proxy.middleware.headers;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_HEADERS;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final MultiMap requestHeaders = new HeadersMultiMap();
        final MultiMap responseHeaders = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_REQUEST) != null) {
            middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_REQUEST).forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    requestHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    requestHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    LOGGER.warn("Invalid header value type: '{}'", entry.getValue());
                }
            });
        }

        if (middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_RESPONSE) != null) {
            middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_RESPONSE).forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    responseHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    responseHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    LOGGER.warn("Invalid header value type: '{}'", entry.getValue());
                }
            });
        }

        LOGGER.debug("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_HEADERS);
        return Future.succeededFuture(new HeaderMiddleware(name, requestHeaders, responseHeaders));
    }

}
