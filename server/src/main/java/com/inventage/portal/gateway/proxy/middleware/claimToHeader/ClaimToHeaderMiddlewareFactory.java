package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
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
 * Factory for {@link ClaimToHeaderMiddleware}.
 */
public class ClaimToHeaderMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "claimToHeader";
    public static final String PATH = "claimPath";
    public static final String NAME = "headerName";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimToHeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(PATH, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Class<ClaimToHeaderMiddlewareOptions> modelType() {
        return ClaimToHeaderMiddlewareOptions.class;
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final ClaimToHeaderMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new ClaimToHeaderMiddleware(name, options.getPath(), options.getName()));
    }
}
