package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link BearerOnlyMiddleware}.
 */
public class BearerOnlyMiddlewareFactory extends WithAuthHandlerMiddlewareFactoryBase {

    // schema
    public static final String BEARER_ONLY = "bearerOnly";
    public static final String BEARER_ONLY_OPTIONAL = "optional";

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareFactory.class);

    @Override
    public String provides() {
        return BEARER_ONLY;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return super.optionsSchema()
            .property(BEARER_ONLY_OPTIONAL, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH));
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return super.validate(options);
    }

    @Override
    protected Middleware create(Vertx vertx, String name, JWKAccessibleAuthHandler authHandler, JsonObject middlewareConfig) {
        final String optionalStr = middlewareConfig.getString(BEARER_ONLY_OPTIONAL);
        final boolean optional = optionalStr != null ? Boolean.parseBoolean(optionalStr) : false;

        final Middleware bearerOnlyMiddleware = new BearerOnlyMiddleware(name, authHandler, optional);
        LOGGER.debug("Created '{}' middleware", BEARER_ONLY);
        return bearerOnlyMiddleware;
    }
}
