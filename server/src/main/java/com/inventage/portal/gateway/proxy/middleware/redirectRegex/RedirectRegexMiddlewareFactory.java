package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
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
    public static final String REDIRECT_REGEX = "redirectRegex";
    public static final String REDIRECT_REGEX_REGEX = "regex";
    public static final String REDIRECT_REGEX_REPLACEMENT = "replacement";

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddlewareFactory.class);

    @Override
    public String provides() {
        return REDIRECT_REGEX;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(REDIRECT_REGEX_REGEX, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(REDIRECT_REGEX_REPLACEMENT, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", REDIRECT_REGEX);
        return Future.succeededFuture(new RedirectRegexMiddleware(
            name,
            middlewareConfig.getString(REDIRECT_REGEX_REGEX),
            middlewareConfig.getString(REDIRECT_REGEX_REPLACEMENT)));
    }
}
