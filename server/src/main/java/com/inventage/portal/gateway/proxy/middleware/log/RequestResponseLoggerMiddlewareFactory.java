package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
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

    // defaults
    public static final JsonArray DEFAULT_CONTENT_TYPES_TO_LOG = JsonArray.of();
    public static final boolean DEFAULT_LOGGING_REQUEST_ENABLED = true;
    public static final boolean DEFAULT_LOGGING_RESPONSE_ENABLED = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddlewareFactory.class);

    @Override
    public String provides() {
        return REQUEST_RESPONSE_LOGGER;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(REQUEST_RESPONSE_LOGGER_FILTER_REGEX, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(REQUEST_RESPONSE_LOGGER_CONTENT_TYPES, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED, Schemas.booleanSchema()
                .defaultValue(DEFAULT_LOGGING_REQUEST_ENABLED))
            .optionalProperty(REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED, Schemas.booleanSchema()
                .defaultValue(DEFAULT_LOGGING_RESPONSE_ENABLED))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<RequestResponseLoggerMiddlewareOptions> modelType() {
        return RequestResponseLoggerMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final RequestResponseLoggerMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}' middleware successfully", REQUEST_RESPONSE_LOGGER);
        return Future.succeededFuture(
            new RequestResponseLoggerMiddleware(name, options.getFilterRegex(), options.getContentTypes(), options.isRequestEnabled(), options.isResponseEnabled()));
    }

}
