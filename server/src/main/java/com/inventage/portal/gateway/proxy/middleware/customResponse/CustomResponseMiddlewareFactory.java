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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link CustomResponseMiddleware}.
 */
public class CustomResponseMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String CUSTOM_RESPONSE = "customResponse";
    public static final String CUSTOM_RESPONSE_CONTENT = "content";
    public static final String CUSTOM_RESPONSE_STATUS_CODE = "statusCode";
    public static final String CUSTOM_RESPONSE_HEADERS = "headers";

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomResponseMiddlewareFactory.class);

    @Override
    public String provides() {
        return CUSTOM_RESPONSE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(CUSTOM_RESPONSE_STATUS_CODE, Schemas.intSchema()
                .withKeyword(KEYWORD_INT_MIN, HTTP_STATUS_CODE_MIN)
                .withKeyword(KEYWORD_INT_MAX, HTTP_STATUS_CODE_MAX))
            .requiredProperty(CUSTOM_RESPONSE_CONTENT, Schemas.stringSchema())
            .optionalProperty(CUSTOM_RESPONSE_HEADERS, Schemas.objectSchema()
                .additionalProperties(Schemas.stringSchema()
                    .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
                .allowAdditionalProperties(false))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", CUSTOM_RESPONSE);

        final String content = middlewareConfig.getString(CUSTOM_RESPONSE_CONTENT);
        final Integer statusCode = middlewareConfig.getInteger(CUSTOM_RESPONSE_STATUS_CODE);
        final MultiMap headers = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(CUSTOM_RESPONSE_HEADERS) != null) {
            middlewareConfig.getJsonObject(CUSTOM_RESPONSE_HEADERS).forEach(entry -> {
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
