package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

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

/**
 * Factory for {@link ReplacePathRegexMiddleware}.
 */
public class ReplacePathRegexMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String REPLACE_PATH_REGEX = "replacePathRegex";
    public static final String REPLACE_PATH_REGEX_REGEX = "regex";
    public static final String REPLACE_PATH_REGEX_REPLACEMENT = "replacement";

    @Override
    public String provides() {
        return REPLACE_PATH_REGEX;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(REPLACE_PATH_REGEX_REGEX, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(REPLACE_PATH_REGEX_REPLACEMENT, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return ReplacePathRegexMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", REPLACE_PATH_REGEX);
        return Future.succeededFuture(new ReplacePathRegexMiddleware(
            name,
            middlewareConfig.getString(REPLACE_PATH_REGEX_REGEX),
            middlewareConfig.getString(REPLACE_PATH_REGEX_REPLACEMENT)));
    }
}
