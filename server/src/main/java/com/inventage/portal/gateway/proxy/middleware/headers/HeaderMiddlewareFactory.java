package com.inventage.portal.gateway.proxy.middleware.headers;

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
 * Factory for {@link HeaderMiddleware}.
 */
public class HeaderMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String HEADERS = "headers";
    public static final String HEADERS_REQUEST = "customRequestHeaders";
    public static final String HEADERS_RESPONSE = "customResponseHeaders";

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return HEADERS;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(HEADERS_REQUEST, Schemas.objectSchema()
                .additionalProperties(Schemas.stringSchema()
                    .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
                .allowAdditionalProperties(false))
            .optionalProperty(HEADERS_RESPONSE, Schemas.objectSchema()
                .additionalProperties(Schemas.stringSchema()
                    .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
                .allowAdditionalProperties(false))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonObject requestHeaders = options.getJsonObject(HEADERS_REQUEST);
        if (requestHeaders != null) {
            if (requestHeaders.isEmpty()) {
                return Future.failedFuture(String.format("%s: Empty request headers defined", HEADERS));
            }
        }

        final JsonObject responseHeaders = options.getJsonObject(HEADERS_RESPONSE);
        if (responseHeaders != null) {
            if (responseHeaders.isEmpty()) {
                return Future.failedFuture(String.format("%s: Empty response headers defined", HEADERS));
            }
        }

        if (requestHeaders == null && responseHeaders == null) {
            return Future.failedFuture(
                String.format("%s: at least one response or request header has to be defined", HEADERS));
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final MultiMap requestHeaders = new HeadersMultiMap();
        final MultiMap responseHeaders = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(HEADERS_REQUEST) != null) {
            middlewareConfig.getJsonObject(HEADERS_REQUEST).forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    requestHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    requestHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    LOGGER.warn("Invalid header value type: '{}'", entry.getValue());
                }
            });
        }

        if (middlewareConfig.getJsonObject(HEADERS_RESPONSE) != null) {
            middlewareConfig.getJsonObject(HEADERS_RESPONSE).forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    responseHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    responseHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    LOGGER.warn("Invalid header value type: '{}'", entry.getValue());
                }
            });
        }

        LOGGER.debug("Created '{}' middleware successfully", HEADERS);
        return Future.succeededFuture(new HeaderMiddleware(name, requestHeaders, responseHeaders));
    }

}
