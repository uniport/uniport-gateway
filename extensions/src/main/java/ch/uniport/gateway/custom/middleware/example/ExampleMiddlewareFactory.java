package ch.uniport.gateway.custom.middleware.example;

import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for adding the additional routes to the router
 */
public class ExampleMiddlewareFactory implements MiddlewareFactory {

    public static final String TYPE = "example";
    public static final String HEADER_KEY = "key";
    public static final String HEADER_VALUE = "value";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(HEADER_KEY, Schemas.stringSchema())
            .requiredProperty(HEADER_VALUE, Schemas.stringSchema())
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<ExampleMiddlewareOptions> modelType() {
        return ExampleMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final ExampleMiddlewareOptions options = castOptions(config, modelType());

        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new ExampleMiddleware(name, options.getHeaderKey(), options.getHeaderValue()));
    }
}
