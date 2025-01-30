package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link SessionBagMiddleware}.
 */
public class SessionBagMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String SESSION_BAG = "sessionBag";
    public static final String SESSION_BAG_COOKIE_NAME = "cookieName";
    public static final String SESSION_BAG_WHITELISTED_COOKIES = "whitelistedCookies";
    public static final String SESSION_BAG_WHITELISTED_COOKIE_NAME = "name";
    public static final String SESSION_BAG_WHITELISTED_COOKIE_PATH = "path";

    // defaults
    private static final String DEFAULT_SESSION_COOKIE_NAME = SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddlewareFactory.class);

    @Override
    public String provides() {
        return SESSION_BAG;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(SESSION_BAG_COOKIE_NAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(SESSION_BAG_WHITELISTED_COOKIES, Schemas.arraySchema())
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonArray whitelistedCookies = options.getJsonArray(SESSION_BAG_WHITELISTED_COOKIES);
        if (whitelistedCookies == null) {
            return Future.failedFuture("No whitelisted cookies defined.");
        }
        for (int j = 0; j < whitelistedCookies.size(); j++) {
            final JsonObject whitelistedCookie = whitelistedCookies.getJsonObject(j);
            if (!whitelistedCookie.containsKey(SESSION_BAG_WHITELISTED_COOKIE_NAME)
                || whitelistedCookie.getString(SESSION_BAG_WHITELISTED_COOKIE_NAME)
                    .isEmpty()) {
                return Future.failedFuture("whitelisted cookie name has to contain a value");
            }
            if (!whitelistedCookie.containsKey(SESSION_BAG_WHITELISTED_COOKIE_PATH)
                || whitelistedCookie.getString(SESSION_BAG_WHITELISTED_COOKIE_PATH)
                    .isEmpty()) {
                return Future.failedFuture("whitelisted cookie path has to contain a value");
            }
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareOptions) {
        return this.create(vertx, name, middlewareOptions);
    }

    public Future<Middleware> create(Vertx vertx, String name, JsonObject middlewareOptions) {
        final JsonArray whitelistedCookies = middlewareOptions.getJsonArray(SESSION_BAG_WHITELISTED_COOKIES);

        String sessionCookieName = middlewareOptions.getString(SESSION_BAG_COOKIE_NAME);
        if (sessionCookieName == null) {
            sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;
        }
        LOGGER.info("Created '{}' middleware successfully", SESSION_BAG);
        return Future.succeededFuture(new SessionBagMiddleware(name, whitelistedCookies, sessionCookieName));
    }

}
