package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.publickeysReconciler.JWTAuthPublicKeysReconcilerHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WithAuthHandlerMiddlewareFactoryBase implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(WithAuthHandlerMiddlewareFactoryBase.class);

    public static final boolean DEFAULT_RECONCILATION_ENABLED_VALUE = true;
    public static final long DEFAULT_RECONCILATION_INTERVAL_MS = 60_000;

    /**
     * Creates the actual middleware.
     *
     * @param authHandler
     *            The {@link AuthenticationHandler} to use in the middleware
     * @param middlewareConfig
     *            The config for the middleware
     * @return Your {@link Middleware}
     */
    protected abstract Middleware create(String name, AuthenticationHandler authHandler, JsonObject middlewareConfig);

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final Promise<Middleware> promise = Promise.promise();
        this.create(vertx, name, router, middlewareConfig, promise);
        return promise.future();
    }

    public void create(Vertx vertx, String name, Router router, JsonObject middlewareConfig, Handler<AsyncResult<Middleware>> handler) {
        LOGGER.debug("Creating '{}' middleware", provides());

        final String issuer = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER);
        final JsonArray audience = middlewareConfig
            .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE);
        final JsonArray additionalClaims = middlewareConfig
            .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_CLAIMS);
        final JsonArray publicKeySources = middlewareConfig
            .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEYS);
        final JsonArray additionalIssuers = middlewareConfig
            .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ADDITIONAL_ISSUERS);

        final JsonObject publicKeysReconcilation = middlewareConfig.getJsonObject(
            DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILATION, new JsonObject());
        final boolean publicKeysReconcilationEnabled = publicKeysReconcilation
            .getBoolean(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILATION_ENABLED, DEFAULT_RECONCILATION_ENABLED_VALUE);
        final long publicKeysReconcilationIntervalMs = publicKeysReconcilation
            .getLong(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILATION_INTERVAL_MS, DEFAULT_RECONCILATION_INTERVAL_MS);

        final JWTOptions jwtOptions = new JWTOptions();
        if (issuer != null) {
            jwtOptions.setIssuer(issuer);
            LOGGER.debug("With issuer '{}'", issuer);
        }
        if (audience != null) {
            jwtOptions.setAudience(audience.getList());
            LOGGER.debug("With audience '{}'", audience);
        }

        final JWTAuthMultipleIssuersOptions additionalIssuersOptions = new JWTAuthMultipleIssuersOptions();
        if (additionalIssuers != null) {
            additionalIssuersOptions.setAdditionalIssuers(additionalIssuers);
            LOGGER.debug("With additional issuers '{}'", additionalIssuers);
        }

        final JWTAuthAdditionalClaimsOptions additionalClaimsOptions = new JWTAuthAdditionalClaimsOptions();
        if (additionalClaims != null) {
            additionalClaimsOptions.setAdditionalClaims(additionalClaims);
            LOGGER.debug("With claims '{}'", additionalClaims);
        }

        JWTAuthPublicKeysReconcilerHandler.fetchPublicKeys(vertx, publicKeySources)
            .onSuccess(authOpts -> {
                final JWTAuthOptions jwtAuthOptions = new JWTAuthOptions(authOpts).setJWTOptions(jwtOptions);

                final JWTAuthPublicKeysReconcilerHandler reconciler = JWTAuthPublicKeysReconcilerHandler.create(
                    vertx, jwtAuthOptions, additionalIssuersOptions, additionalClaimsOptions, publicKeySources, publicKeysReconcilationEnabled, publicKeysReconcilationIntervalMs);

                handler.handle(Future.succeededFuture(create(name, reconciler, middlewareConfig)));
            })
            .onFailure(err -> handler.handle(Future.failedFuture(err.getMessage())));

    }
}
