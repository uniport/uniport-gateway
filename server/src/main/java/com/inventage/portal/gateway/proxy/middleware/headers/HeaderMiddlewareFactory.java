package com.inventage.portal.gateway.proxy.middleware.headers;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
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
                .with(Keywords.minProperties(1))
                .additionalProperties(Schemas.anyOf(
                    Schemas.stringSchema(),
                    Schemas.arraySchema()
                        .items(Schemas.stringSchema()
                            .with(Keywords.minLength(1))))))
            .optionalProperty(HEADERS_RESPONSE, Schemas.objectSchema()
                .with(Keywords.minProperties(1))
                .additionalProperties(Schemas.anyOf(
                    Schemas.stringSchema(),
                    Schemas.arraySchema()
                        .items(Schemas.stringSchema()
                            .with(Keywords.minLength(1))))))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonObject requestHeaders = options.getJsonObject(HEADERS_REQUEST);
        final JsonObject responseHeaders = options.getJsonObject(HEADERS_RESPONSE);
        if (requestHeaders == null && responseHeaders == null) {
            return Future.failedFuture(
                String.format("%s: at least one response or request header has to be defined", HEADERS));
        }

        logDefaultIfNotConfigured(LOGGER, options, HEADERS_REQUEST, null);
        logDefaultIfNotConfigured(LOGGER, options, HEADERS_RESPONSE, null);

        return Future.succeededFuture();
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return HeaderMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final MultiMap requestHeaders = new HeadersMultiMap();
        final MultiMap responseHeaders = new HeadersMultiMap();

        if (middlewareConfig.getJsonObject(HEADERS_REQUEST) != null) {
            for (Entry<String, Object> entry : middlewareConfig.getJsonObject(HEADERS_REQUEST).getMap().entrySet()) {
                if (entry.getValue() instanceof String) {
                    requestHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    requestHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    return Future.failedFuture(new IllegalStateException(String.format("Invalid header value type: '%s'", entry.getValue())));
                }
            }
        }

        if (middlewareConfig.getJsonObject(HEADERS_RESPONSE) != null) {
            for (Entry<String, Object> entry : middlewareConfig.getJsonObject(HEADERS_RESPONSE).getMap().entrySet()) {
                if (entry.getValue() instanceof String) {
                    responseHeaders.set(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Iterable) {
                    responseHeaders.set(entry.getKey(), (Iterable<String>) entry.getValue());
                } else {
                    return Future.failedFuture(new IllegalStateException(String.format("Invalid header value type: '%s'", entry.getValue())));
                }
            }
        }

        LOGGER.debug("Created '{}' middleware successfully", HEADERS);
        return Future.succeededFuture(new HeaderMiddleware(name, requestHeaders, responseHeaders));
    }

}
