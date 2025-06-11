package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
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
    public static final String TYPE = "responseSessionCookieRemoval";
    public static final String SESSION_COOKIE_NAME = "name";

    public static final String DEFAULT_SESSION_COOKIE_NAME = SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(SESSION_COOKIE_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_SESSION_COOKIE_NAME))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<ResponseSessionCookieRemovalMiddlewareOptions> modelType() {
        return ResponseSessionCookieRemovalMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final ResponseSessionCookieRemovalMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(new ResponseSessionCookieRemovalMiddleware(name, options.getSessionCookieName()));
    }
}
