package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly;

import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import ch.uniport.gateway.proxy.middleware.authorization.JWTAuthVerifierMiddlewareFactoryBase;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link BearerOnlyMiddleware}.
 */
public class BearerOnlyMiddlewareFactory extends JWTAuthVerifierMiddlewareFactoryBase {

    // schema
    public static final String TYPE = "bearerOnly";
    public static final String OPTIONAL = "optional";

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return super.optionsSchema()
            .optionalProperty(OPTIONAL, Schemas.anyOf(
                Schemas.booleanSchema()
                    .defaultValue(AbstractBearerOnlyMiddlewareOptions.DEFAULT_OPTIONAL),
                Schemas.stringSchema()
                    .with(Keywords.pattern(DynamicConfiguration.ENV_VARIABLE_PATTERN))));

    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return super.validate(options);
    }

    @Override
    public Class<BearerOnlyMiddlewareOptions> modelType() {
        return BearerOnlyMiddlewareOptions.class;
    }

    @Override
    protected Middleware create(
        Vertx vertx, String name, JWKAccessibleAuthHandler authHandler,
        MiddlewareOptionsModel config
    ) {
        final BearerOnlyMiddlewareOptions options = castOptions(config, modelType());

        final Middleware bearerOnlyMiddleware = new BearerOnlyMiddleware(name, authHandler, options.isOptional());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return bearerOnlyMiddleware;
    }
}
