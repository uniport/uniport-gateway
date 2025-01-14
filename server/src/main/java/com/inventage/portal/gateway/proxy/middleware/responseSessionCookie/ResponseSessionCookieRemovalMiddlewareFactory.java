package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

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
 * Factory for {@link ResponseSessionCookieRemovalMiddleware}.
 */
public class ResponseSessionCookieRemovalMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL = "responseSessionCookieRemoval";
    public static final String MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME = "name";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String sessionCookieName = middlewareConfig.getString(MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME);
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL);
        return Future.succeededFuture(new ResponseSessionCookieRemovalMiddleware(name, sessionCookieName));
    }
}
