package com.inventage.portal.gateway.proxy.middleware.customResponse;

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

/**
 * Configures the custom response middleware.
 */
public class CustomResponseMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomResponseMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_CUSTOM_RESPONSE;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_CUSTOM_RESPONSE);

        final String content = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_CUSTOM_RESPONSE_CONTENT);
        final Integer statusCode = middlewareConfig.getInteger(DynamicConfiguration.MIDDLEWARE_CUSTOM_RESPONSE_STATUS_CODE);
        final MultiMap headers = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_CUSTOM_RESPONSE_HEADERS) != null) {
            middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_CUSTOM_RESPONSE_HEADERS).forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    headers.set(entry.getKey(), (String) entry.getValue());
                } else {
                    LOGGER.warn("Invalid header value type: '{}'", entry.getValue());
                }
            });
        }

        return Future.succeededFuture(new CustomResponseMiddleware(name, content, statusCode, headers));
    }
}
