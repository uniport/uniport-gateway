package ch.uniport.gateway.proxy.middleware.authorization.passAuthorization;

import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link PassAuthorizationMiddleware}.
 */
public class PassAuthorizationMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    // schema
    public static final String TYPE = "passAuthorization";
    public static final String SESSION_SCOPE = "sessionScope";

    private static final Logger LOGGER = LoggerFactory.getLogger(PassAuthorizationMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return super.optionsSchema()
            .requiredProperty(SESSION_SCOPE, Schemas.stringSchema()
                .with(Keywords.minLength(1)));
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return super.validate(options)
            .compose(v -> {
                return Future.succeededFuture();
            });
    }

    @Override
    public Class<PassAuthorizationMiddlewareOptions> modelType() {
        return PassAuthorizationMiddlewareOptions.class;
    }

    @Override
    protected Middleware create(
        Vertx vertx, String name, JWKAccessibleAuthHandler authHandler,
        MiddlewareOptionsModel config
    ) {
        final PassAuthorizationMiddlewareOptions options = castOptions(config, modelType());
        final String sessionScope = options.getSessionScope();

        final Middleware passAuthorizationMiddleware = new PassAuthorizationMiddleware(vertx, name, sessionScope,
            authHandler);
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return passAuthorizationMiddleware;
    }

}
