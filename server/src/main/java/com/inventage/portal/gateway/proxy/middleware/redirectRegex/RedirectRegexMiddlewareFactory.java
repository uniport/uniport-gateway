package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

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
 * Factory for {@link RedirectRegexMiddleware}.
 */
public class RedirectRegexMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "redirectRegex";
    public static final String REGEX = "regex";
    public static final String REPLACEMENT = "replacement";

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddlewareFactory.class);

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
    public Class<RedirectRegexMiddlewareOptions> modelType() {
        return RedirectRegexMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final RedirectRegexMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new RedirectRegexMiddleware(
                name,
                options.getRegex(),
                options.getReplacement()));
    }
}
