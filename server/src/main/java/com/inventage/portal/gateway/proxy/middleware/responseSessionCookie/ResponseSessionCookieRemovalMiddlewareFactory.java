package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
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
 * Factory for {@link ResponseSessionCookieRemovalMiddleware}.
 */
public class ResponseSessionCookieRemovalMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String RESPONSE_SESSION_COOKIE_REMOVAL = "responseSessionCookieRemoval";
    public static final String RESPONSE_SESSION_COOKIE_REMOVAL_NAME = "name";

    public static final String DEFAULT_SESSION_COOKIE_NAME = SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddlewareFactory.class);

    @Override
    public String provides() {
        return RESPONSE_SESSION_COOKIE_REMOVAL;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(RESPONSE_SESSION_COOKIE_REMOVAL_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_SESSION_COOKIE_NAME))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, RESPONSE_SESSION_COOKIE_REMOVAL_NAME, DEFAULT_SESSION_COOKIE_NAME);

        return Future.succeededFuture();
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return ResponseSessionCookieRemovalMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String sessionCookieName = middlewareConfig.getString(RESPONSE_SESSION_COOKIE_REMOVAL_NAME, DEFAULT_SESSION_COOKIE_NAME);

        LOGGER.debug("Created '{}' middleware successfully", RESPONSE_SESSION_COOKIE_REMOVAL);
        return Future.succeededFuture(new ResponseSessionCookieRemovalMiddleware(name, sessionCookieName));
    }
}
