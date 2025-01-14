package com.inventage.portal.gateway.proxy.middleware.customResponse;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link CustomResponseMiddleware}.
 */
public class CustomResponseMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_CUSTOM_RESPONSE = "customResponse";
    public static final String MIDDLEWARE_CUSTOM_RESPONSE_CONTENT = "content";
    public static final String MIDDLEWARE_CUSTOM_RESPONSE_STATUS_CODE = "statusCode";
    public static final String MIDDLEWARE_CUSTOM_RESPONSE_HEADERS = "headers";

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomResponseMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_CUSTOM_RESPONSE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(MIDDLEWARE_CUSTOM_RESPONSE_CONTENT, Schemas.stringSchema())
            .property(MIDDLEWARE_CUSTOM_RESPONSE_STATUS_CODE, Schemas.intSchema()
                .withKeyword(KEYWORD_INT_MIN, HTTP_STATUS_CODE_MIN)
                .withKeyword(KEYWORD_INT_MAX, HTTP_STATUS_CODE_MAX))
            .optionalProperty(MIDDLEWARE_CUSTOM_RESPONSE_HEADERS, Schemas.objectSchema())
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final Integer statusCode = options.getInteger(MIDDLEWARE_CUSTOM_RESPONSE_STATUS_CODE);
        if (statusCode == null) {
            return Future.failedFuture("Status code can only be of type integer");
        }

        final JsonObject headers = options.getJsonObject(MIDDLEWARE_CUSTOM_RESPONSE_HEADERS);
        if (headers != null) {
            for (Entry<String, Object> entry : headers) {
                if (entry.getKey() == null || !(entry.getValue() instanceof String)) {
                    return Future.failedFuture("Response header and value can only be of type string");
                }
            }
        }
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_CUSTOM_RESPONSE);

        final String content = middlewareConfig.getString(MIDDLEWARE_CUSTOM_RESPONSE_CONTENT);
        final Integer statusCode = middlewareConfig.getInteger(MIDDLEWARE_CUSTOM_RESPONSE_STATUS_CODE);
        final MultiMap headers = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(MIDDLEWARE_CUSTOM_RESPONSE_HEADERS) != null) {
            middlewareConfig.getJsonObject(MIDDLEWARE_CUSTOM_RESPONSE_HEADERS).forEach(entry -> {
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
