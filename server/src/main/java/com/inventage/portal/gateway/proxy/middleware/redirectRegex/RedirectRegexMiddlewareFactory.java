package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link RedirectRegexMiddleware}.
 */
public class RedirectRegexMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_REDIRECT_REGEX = "redirectRegex";
    public static final String MIDDLEWARE_REDIRECT_REGEX_REGEX = "regex";
    public static final String MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT = "replacement";

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_REDIRECT_REGEX;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(MIDDLEWARE_REDIRECT_REGEX_REGEX, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final String regex = options.getString(MIDDLEWARE_REDIRECT_REGEX_REGEX);
        if (regex == null || regex.length() == 0) {
            return Future.failedFuture("No regex defined");
        }

        final String replacement = options.getString(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT);
        if (replacement == null || replacement.length() == 0) {
            return Future.failedFuture("No replacement defined");
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_REDIRECT_REGEX);
        return Future.succeededFuture(new RedirectRegexMiddleware(
            name,
            middlewareConfig.getString(MIDDLEWARE_REDIRECT_REGEX_REGEX),
            middlewareConfig.getString(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT)));
    }
}
