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
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link HeaderMiddleware}.
 */
public class HeaderMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_HEADERS = "headers";
    public static final String MIDDLEWARE_HEADERS_REQUEST = "customRequestHeaders";
    public static final String MIDDLEWARE_HEADERS_RESPONSE = "customResponseHeaders";

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_HEADERS;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(MIDDLEWARE_HEADERS_REQUEST, Schemas.objectSchema())
            .property(MIDDLEWARE_HEADERS_RESPONSE, Schemas.objectSchema())
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonObject requestHeaders = options.getJsonObject(MIDDLEWARE_HEADERS_REQUEST);
        if (requestHeaders != null) {
            if (requestHeaders.isEmpty()) {
                return Future.failedFuture(String.format("%s: Empty request headers defined", MIDDLEWARE_HEADERS));
            }

            for (Entry<String, Object> entry : requestHeaders) {
                if (entry.getKey() == null || !(entry.getValue() instanceof String)) {
                    return Future.failedFuture(String
                        .format("%s: Request header and value can only be of type string", MIDDLEWARE_HEADERS));
                }
            }
        }

        final JsonObject responseHeaders = options.getJsonObject(MIDDLEWARE_HEADERS_RESPONSE);
        if (responseHeaders != null) {
            if (responseHeaders.isEmpty()) {
                return Future.failedFuture(String.format("%s: Empty response headers defined", MIDDLEWARE_HEADERS));
            }

            for (Entry<String, Object> entry : responseHeaders) {
                if (entry.getKey() == null || !(entry.getValue() instanceof String)) {
                    return Future.failedFuture(String
                        .format("%s: Response header and value can only be of type string", MIDDLEWARE_HEADERS));
                }
            }
        }

        if (requestHeaders == null && responseHeaders == null) {
            return Future.failedFuture(
                String.format("%s: at least one response or request header has to be defined", MIDDLEWARE_HEADERS));
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final MultiMap requestHeaders = new HeadersMultiMap();
        final MultiMap responseHeaders = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(MIDDLEWARE_HEADERS_REQUEST) != null) {
            middlewareConfig.getJsonObject(MIDDLEWARE_HEADERS_REQUEST).forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    requestHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    requestHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    LOGGER.warn("Invalid header value type: '{}'", entry.getValue());
                }
            });
        }

        if (middlewareConfig.getJsonObject(MIDDLEWARE_HEADERS_RESPONSE) != null) {
            middlewareConfig.getJsonObject(MIDDLEWARE_HEADERS_RESPONSE).forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    responseHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    responseHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    LOGGER.warn("Invalid header value type: '{}'", entry.getValue());
                }
            });
        }

        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_HEADERS);
        return Future.succeededFuture(new HeaderMiddleware(name, requestHeaders, responseHeaders));
    }

}
