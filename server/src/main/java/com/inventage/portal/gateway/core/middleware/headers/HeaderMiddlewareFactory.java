package com.inventage.portal.gateway.core.middleware.headers;

import java.util.HashMap;
import java.util.Map;
import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class HeaderMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(HeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_HEADERS;
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {
        // TODO read headers from config and put into maps
        Map<String, String> requestHeaders = new HashMap<>();
        Map<String, String> responseHeaders = new HashMap<>();
        return new HeaderMiddleware(requestHeaders, responseHeaders);
    }

}


