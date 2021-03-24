package com.inventage.portal.gateway.proxy.middleware.headers;

import java.util.HashMap;
import java.util.Map;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class HeaderMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_HEADERS;
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        Map<String, String> requestHeaders = new HashMap<>();
        Map<String, String> responseHeaders = new HashMap<>();

        middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_REQUEST)
                .forEach(entry -> {
                    requestHeaders.put(entry.getKey(), (String) entry.getValue());
                });
        middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_HEADERS_RESPONSE)
                .forEach(entry -> {
                    responseHeaders.put(entry.getKey(), (String) entry.getValue());
                });

        return new HeaderMiddleware(requestHeaders, responseHeaders);
    }

}


