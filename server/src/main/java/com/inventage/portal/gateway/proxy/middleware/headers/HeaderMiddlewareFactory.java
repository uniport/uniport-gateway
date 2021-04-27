package com.inventage.portal.gateway.proxy.middleware.headers;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class HeaderMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_HEADERS;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        MultiMap requestHeaders = new HeadersMultiMap();
        MultiMap responseHeaders = new HeadersMultiMap();

        middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_REQUEST).forEach(entry -> {
            if (entry.getValue() instanceof String) {
                requestHeaders.set(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Iterable) {
                requestHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
            } else {
                LOGGER.warn("create: Invalid header value type: '{}'", entry.getValue());
            }
        });
        middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_RESPONSE).forEach(entry -> {
            if (entry.getValue() instanceof String) {
                responseHeaders.set(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Iterable) {
                responseHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
            } else {
                LOGGER.warn("create: Invalid header value type: '{}'", entry.getValue());
            }
        });

        LOGGER.debug("create: Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_HEADERS);
        return Future.succeededFuture(new HeaderMiddleware(requestHeaders, responseHeaders));
    }

}
