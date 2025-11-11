package ch.uniport.gateway.proxy.middleware.replacePathRegex;

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

/**
 * Factory for {@link ReplacePathRegexMiddleware}.
 */
public class ReplacePathRegexMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "replacePathRegex";
    public static final String REGEX = "regex";
    public static final String REPLACEMENT = "replacement";

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(REGEX, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(REPLACEMENT, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<ReplacePathRegexMiddlewareOptions> modelType() {
        return ReplacePathRegexMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final ReplacePathRegexMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new ReplacePathRegexMiddleware(
                name,
                options.getRegex(),
                options.getReplacement()));
    }
}
