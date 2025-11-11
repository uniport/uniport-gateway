package ch.uniport.gateway.proxy.middleware.log;

import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
    public static final String TYPE = "requestResponseLogger";
    public static final String FILTER_REGEX = "uriWithoutLoggingRegex";
    public static final String CONTENT_TYPES = "contentTypes";
    public static final String LOGGING_REQUEST_ENABLED = "loggingRequestEnabled";
    public static final String LOGGING_RESPONSE_ENABLED = "loggingResponseEnabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(FILTER_REGEX, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(CONTENT_TYPES, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(LOGGING_REQUEST_ENABLED, Schemas.booleanSchema()
                .defaultValue(AbstractRequestResponseLoggerMiddlewareOptions.DEFAULT_LOGGING_REQUEST_ENABLED))
            .optionalProperty(LOGGING_RESPONSE_ENABLED, Schemas.booleanSchema()
                .defaultValue(AbstractRequestResponseLoggerMiddlewareOptions.DEFAULT_LOGGING_RESPONSE_ENABLED))
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
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final RequestResponseLoggerMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new RequestResponseLoggerMiddleware(name, options.getFilterRegex(), options.getContentTypes(),
                options.isRequestEnabled(), options.isResponseEnabled()));
    }

}
