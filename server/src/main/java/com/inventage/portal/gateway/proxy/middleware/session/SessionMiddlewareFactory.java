package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieUtil;
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
 * Factory for {@link SessionMiddleware}.
 */
public class SessionMiddlewareFactory implements MiddlewareFactory {

    // schema
    // session
    public static final String TYPE = "session";
    public static final String SESSION_ID_MIN_LENGTH = "idMinimumLength";
    public static final String SESSION_IDLE_TIMEOUT_IN_MINUTES = "idleTimeoutInMinutes";
    public static final String IGNORE_SESSION_TIMEOUT_RESET_FOR_URI = "uriWithoutSessionTimeoutReset";
    public static final String NAG_HTTPS = "nagHttps";
    public static final String SESSION_LIFETIME_COOKIE = "lifetimeCookie";
    public static final String SESSION_LIFETIME_HEADER = "lifetimeHeader";

    // session cookie
    public static final String SESSION_COOKIE = "cookie";
    public static final String SESSION_COOKIE_HTTP_ONLY = "httpOnly";
    public static final String SESSION_COOKIE_NAME = "name";
    public static final String SESSION_COOKIE_SAME_SITE = "sameSite";
    public static final String SESSION_COOKIE_SECURE = "secure";

    // session store
    public static final String CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS = "clusteredSessionStoreRetryTimeoutInMilliseconds";

    private static final String[] COOKIE_SAME_SITE_POLICIES = new String[] {
        "NONE",
        "STRICT",
        "LAX"
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            // session
            .optionalProperty(SESSION_IDLE_TIMEOUT_IN_MINUTES, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE))
            .optionalProperty(SESSION_ID_MIN_LENGTH, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_ID_MINIMUM_LENGTH))
            .optionalProperty(NAG_HTTPS, Schemas.booleanSchema()
                .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_NAG_HTTPS))
            .optionalProperty(IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            // session lifetime
            .optionalProperty(SESSION_LIFETIME_COOKIE, Schemas.booleanSchema()
                .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_LIFETIME_COOKIE))
            .optionalProperty(SESSION_LIFETIME_HEADER, Schemas.booleanSchema()
                .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_LIFETIME_HEADER))
            // session cookie
            .optionalProperty(SESSION_COOKIE, Schemas.objectSchema()
                .optionalProperty(SESSION_COOKIE_NAME, Schemas.stringSchema()
                    .with(Keywords.minLength(1))
                    .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME))
                .optionalProperty(SESSION_COOKIE_HTTP_ONLY, Schemas.booleanSchema()
                    .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_HTTP_ONLY))
                .optionalProperty(SESSION_COOKIE_SECURE, Schemas.booleanSchema()
                    .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_SECURE))
                .optionalProperty(SESSION_COOKIE_SAME_SITE, Schemas.enumSchema((Object[]) COOKIE_SAME_SITE_POLICIES)
                    .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_SAME_SITE))
                .allowAdditionalProperties(false))
            // session store
            .optionalProperty(CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(AbstractSessionMiddlewareOptions.DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        if (options.containsKey(SESSION_COOKIE) &&
            options.getJsonObject(SESSION_COOKIE).containsKey(SESSION_COOKIE_NAME) &&
            !CookieUtil.isValidCookieName(LOGGER, options.getJsonObject(SESSION_COOKIE).getString(SESSION_COOKIE_NAME))) {
            return Future.failedFuture("cookie name is invalid");
        }
        return Future.succeededFuture();
    }

    @Override
    public Class<SessionMiddlewareOptions> modelType() {
        return SessionMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final SessionMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new SessionMiddleware(
                vertx,
                name,
                // session
                options.getIdMinLength(),
                options.getIdleTimeoutMinutes(),
                options.getIgnoreSessionTimeoutResetForURI(),
                options.nagHttps(),
                // session cookie
                options.getSessionCookie(),
                // lifetime
                options.useLifetimeHeader(),
                options.getLifetimeHeader(),
                options.useLifetimeCookie(),
                options.getLifetimeCookie(),
                // session store
                options.getClusteredSessionStoreRetryTimeoutMs()));
    }
}
