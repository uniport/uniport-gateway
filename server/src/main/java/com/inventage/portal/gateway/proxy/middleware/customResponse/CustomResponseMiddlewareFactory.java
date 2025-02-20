package com.inventage.portal.gateway.proxy.middleware.customResponse;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
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
    public static final String CUSTOM_RESPONSE = "customResponse";
    public static final String CUSTOM_RESPONSE_CONTENT = "content";
    public static final String CUSTOM_RESPONSE_STATUS_CODE = "statusCode";
    public static final String CUSTOM_RESPONSE_HEADERS = "headers";

    private static final int HTTP_STATUS_CODE_MIN = 100;
    private static final int HTTP_STATUS_CODE_MAX = 599;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomResponseMiddlewareFactory.class);

    @Override
    public String provides() {
        return CUSTOM_RESPONSE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(CUSTOM_RESPONSE_STATUS_CODE, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(HTTP_STATUS_CODE_MIN))
                .with(io.vertx.json.schema.draft7.dsl.Keywords.maximum(HTTP_STATUS_CODE_MAX)))
            .requiredProperty(CUSTOM_RESPONSE_CONTENT, Schemas.stringSchema())
            .optionalProperty(CUSTOM_RESPONSE_HEADERS, Schemas.objectSchema()
                .additionalProperties(Schemas.stringSchema() // technically this should be an array to allow multi header values
                    .with(Keywords.minLength(1))))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, CUSTOM_RESPONSE_HEADERS, null);

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String content = middlewareConfig.getString(CUSTOM_RESPONSE_CONTENT);
        final Integer statusCode = middlewareConfig.getInteger(CUSTOM_RESPONSE_STATUS_CODE);
        final MultiMap headers = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(CUSTOM_RESPONSE_HEADERS) != null) {
            for (Entry<String, Object> entry : middlewareConfig.getJsonObject(CUSTOM_RESPONSE_HEADERS).getMap().entrySet()) {
                if (entry.getValue() instanceof String) {
                    headers.set(entry.getKey(), (String) entry.getValue());
                } else {
                    return Future.failedFuture(new IllegalStateException(String.format("Invalid header value type: '%s'", entry.getValue())));
                }
            }
        }

        LOGGER.debug("Created '{}' middleware successfully", CUSTOM_RESPONSE);
        return Future.succeededFuture(new CustomResponseMiddleware(name, content, statusCode, headers));
    }
}
