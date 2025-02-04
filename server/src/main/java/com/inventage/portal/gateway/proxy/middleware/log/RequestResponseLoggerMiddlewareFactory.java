package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link RequestResponseLoggerMiddleware}.
 */
public class RequestResponseLoggerMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String REQUEST_RESPONSE_LOGGER = "requestResponseLogger";
    public static final String REQUEST_RESPONSE_LOGGER_FILTER_REGEX = "uriWithoutLoggingRegex";
    public static final String REQUEST_RESPONSE_LOGGER_CONTENT_TYPES = "contentTypes";
    public static final String REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED = "loggingRequestEnabled";
    public static final String REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED = "loggingResponseEnabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddlewareFactory.class);

    @Override
    public String provides() {
        return REQUEST_RESPONSE_LOGGER;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(REQUEST_RESPONSE_LOGGER_FILTER_REGEX, Schemas.stringSchema())
            .optionalProperty(REQUEST_RESPONSE_LOGGER_CONTENT_TYPES, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH)))
            .optionalProperty(REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED, Schemas.booleanSchema())
            .optionalProperty(REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED, Schemas.booleanSchema())
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String requestFilterPattern = middlewareConfig.getString(REQUEST_RESPONSE_LOGGER_FILTER_REGEX);
        List<String> contentTypesToLog = null;
        if (middlewareConfig.getJsonArray(REQUEST_RESPONSE_LOGGER_CONTENT_TYPES) != null) {
            contentTypesToLog = middlewareConfig.getJsonArray(REQUEST_RESPONSE_LOGGER_CONTENT_TYPES).getList();
        }
        final Boolean loggingRequestEnabled = middlewareConfig.getBoolean(REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED);
        final Boolean loggingResponseEnabled = middlewareConfig.getBoolean(REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED);

        LOGGER.debug("Created '{}' middleware successfully", REQUEST_RESPONSE_LOGGER);
        return Future.succeededFuture(new RequestResponseLoggerMiddleware(name, requestFilterPattern, contentTypesToLog, loggingRequestEnabled, loggingResponseEnabled));
    }

}
