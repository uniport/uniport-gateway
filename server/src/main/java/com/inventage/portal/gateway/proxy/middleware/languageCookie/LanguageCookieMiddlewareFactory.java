package com.inventage.portal.gateway.proxy.middleware.languageCookie;

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
 * Factory for {@link LanguageCookieMiddleware}.
 */
public class LanguageCookieMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "languageCookie";
    public static final String NAME = "name";

    // defaults
    public static final String DEFAULT_LANGUAGE_COOKIE_NAME = "uniport.language";

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_LANGUAGE_COOKIE_NAME))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<LanguageCookieMiddlewareOptions> modelType() {
        return LanguageCookieMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final LanguageCookieMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}' middleware successfully", TYPE);
        return Future.succeededFuture(
            new LanguageCookieMiddleware(name, options.getCookieName()));
    }
}
