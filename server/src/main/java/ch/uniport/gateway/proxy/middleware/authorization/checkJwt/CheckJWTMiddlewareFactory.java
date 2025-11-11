package ch.uniport.gateway.proxy.middleware.authorization.checkJwt;

import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
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
 * Factory for {@link CheckJWTMiddleware}.
 */
public class CheckJWTMiddlewareFactory extends JWTAuthVerifierMiddlewareFactoryBase {

    // schema
    public static final String TYPE = "checkJWT";
    public static final String SESSION_SCOPE = "sessionScope";

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckJWTMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return super.optionsSchema()
            .requiredProperty(SESSION_SCOPE, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return super.validate(options)
            .compose(v -> {
                return Future.succeededFuture();
            });
    }

    @Override
    public Class<CheckJWTMiddlewareOptions> modelType() {
        return CheckJWTMiddlewareOptions.class;
    }

    @Override
    protected Middleware create(
        Vertx vertx, String name, JWKAccessibleAuthHandler authHandler,
        MiddlewareOptionsModel config
    ) {
        final CheckJWTMiddlewareOptions options = castOptions(config, modelType());
        final String sessionScope = options.getSessionScope();

        final Middleware checkJWT = new CheckJWTMiddleware(vertx, name, sessionScope, authHandler);
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return checkJWT;
    }
}
