package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

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
 * Factory for {@link LanguageCookieMiddleware}.
 */
public class LanguageCookieMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String LANGUAGE_COOKIE = "languageCookie";
    public static final String LANGUAGE_COOKIE_NAME = "name";

    // defaults
    public static final String DEFAULT_LANGUAGE_COOKIE_NAME = "uniport.language";

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddlewareFactory.class);

    @Override
    public String provides() {
        return LANGUAGE_COOKIE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(LANGUAGE_COOKIE_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_LANGUAGE_COOKIE_NAME))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, LANGUAGE_COOKIE_NAME, DEFAULT_LANGUAGE_COOKIE_NAME);

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String languageCookieName = middlewareConfig.getString(LANGUAGE_COOKIE_NAME, DEFAULT_LANGUAGE_COOKIE_NAME);

        LOGGER.debug("Created '{}' middleware successfully", LANGUAGE_COOKIE);
        return Future.succeededFuture(new LanguageCookieMiddleware(name, languageCookieName));
    }
}
