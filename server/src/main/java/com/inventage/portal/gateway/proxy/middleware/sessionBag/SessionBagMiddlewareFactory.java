package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
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
 * Factory for {@link SessionBagMiddleware}.
 */
public class SessionBagMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "sessionBag";
    public static final String SESSION_COOKIE_NAME = "cookieName";
    public static final String WHITELISTED_COOKIES = "whitelistedCookies";
    public static final String WHITELISTED_COOKIE_NAME = "name";
    public static final String WHITELISTED_COOKIE_PATH = "path";

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(SESSION_COOKIE_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(AbstractSessionBagMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME))
            .requiredProperty(WHITELISTED_COOKIES, Schemas.arraySchema()
                .items(Schemas.objectSchema()
                    .requiredProperty(WHITELISTED_COOKIE_NAME, Schemas.stringSchema()
                        .with(Keywords.minLength(1)))
                    .requiredProperty(WHITELISTED_COOKIE_PATH, Schemas.stringSchema()
                        .with(Keywords.minLength(1)))
                    .allowAdditionalProperties(false)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<SessionBagMiddlewareOptions> modelType() {
        return SessionBagMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final SessionBagMiddlewareOptions options = castOptions(config, modelType());
        return this.create(vertx, name, options);
    }

    public Future<Middleware> create(Vertx vertx, String name, SessionBagMiddlewareOptions options) {
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new SessionBagMiddleware(name, options.getWhitelistedCookieOptions(), options.getSessionCookieName()));
    }

}
