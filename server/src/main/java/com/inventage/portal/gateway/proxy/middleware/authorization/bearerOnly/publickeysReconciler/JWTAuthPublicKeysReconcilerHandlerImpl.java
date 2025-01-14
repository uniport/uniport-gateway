package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.publickeysReconciler;

import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker.JWTAuthMultipleIssuersProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates an Bearer Token provided in the Authorzation Headers.
 * The validation includes signature check, issuers, claims
 * The public key used to verify the JWT siganture is fetched periodically, if enabled.
 */
public class JWTAuthPublicKeysReconcilerHandlerImpl implements JWTAuthPublicKeysReconcilerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthPublicKeysReconcilerHandlerImpl.class);

    private final Vertx vertx;
    private final JsonArray publicKeySources;
    private final boolean reconcilationEnabled;
    private final long reconcilationIntervalMs;

    private final JWTOptions jwtOptions;
    private final JWTAuthMultipleIssuersOptions additionalIssuersOptions;
    private final JWTAuthAdditionalClaimsOptions additionalClaimsOptions;

    private JWTAuthOptions jwtAuthOptions;
    private AuthenticationHandler authHandler;
    private long timerId;
    private boolean reconcilerStarted;

    public JWTAuthPublicKeysReconcilerHandlerImpl(
        Vertx vertx,
        JWTAuthOptions jwtAuthOptions,
        JWTAuthMultipleIssuersOptions additionalIssuersOptions,
        JWTAuthAdditionalClaimsOptions additionalClaimsOptions,
        JsonArray publicKeySources,
        boolean reconcilationEnabled,
        long reconcilationIntervalMs
    ) {
        this.vertx = vertx;
        this.jwtAuthOptions = jwtAuthOptions;
        this.jwtOptions = jwtAuthOptions.getJWTOptions();
        this.additionalIssuersOptions = additionalIssuersOptions;
        this.additionalClaimsOptions = additionalClaimsOptions;

        this.publicKeySources = publicKeySources;
        this.reconcilationEnabled = reconcilationEnabled;
        this.reconcilationIntervalMs = reconcilationIntervalMs;

        this.authHandler = createAuthHandlerWithFreshPublicKeys(jwtAuthOptions);
        if (this.reconcilationEnabled) {
            this.startReconciler();
        }
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("Handling '{}'", ctx.request().absoluteURI());

        LOGGER.debug("Handling auth request");
        this.authHandler.handle(ctx);
        LOGGER.debug("Handled auth request");
    }

    private AuthenticationHandler createAuthHandlerWithFreshPublicKeys(JWTAuthOptions authOpts) {
        final JWTAuthOptions authOptions = new JWTAuthOptions(authOpts)
            .setJWTOptions(jwtOptions);
        final JWTAuth authProvider = JWTAuthMultipleIssuersProvider.create(this.vertx, authOptions, this.additionalIssuersOptions);
        final AuthenticationHandler authHandler = JWTAuthAdditionalClaimsHandler.create(authProvider, additionalClaimsOptions, this);

        return authHandler;
    }

    private void startReconciler() {
        if (this.reconcilerStarted) {
            return;
        }

        LOGGER.info("Starting public keys reconciler loop");
        this.timerId = this.vertx.setPeriodic(this.reconcilationIntervalMs, tId -> {
            getOrRefreshPublicKeys();
        });
        this.reconcilerStarted = true;
        LOGGER.debug("Started public keys reconciler loop");

        if (vertx instanceof VertxInternal) {
            ((VertxInternal) vertx).addCloseHook(completionHandler -> {
                LOGGER.warn("Canceling public keys reconciler loop");
                vertx.cancelTimer(this.timerId);
                LOGGER.debug("Canceled public keys reconciler loop");
                completionHandler.handle(Future.succeededFuture());
            });
        } else {
            LOGGER.warn("Failed to register public keys reconciler loop canceller");
        }
    }

    public Future<AuthenticationHandler> getOrRefreshPublicKeys() {
        LOGGER.debug("Refreshing public keys");

        final Promise<AuthenticationHandler> promise = Promise.promise();
        JWTAuthPublicKeysReconcilerHandler.fetchPublicKeys(this.vertx, this.publicKeySources)
            .onSuccess(authOptions -> {
                this.jwtAuthOptions = authOptions;
                this.authHandler = createAuthHandlerWithFreshPublicKeys(authOptions);
                LOGGER.debug("Refreshed public keys");
                promise.complete(this);
            })
            .onFailure(err -> {
                final String errMsg = String.format("Failed to refresh public keys '%s'", err.getMessage());
                LOGGER.warn(errMsg);
                promise.fail(errMsg);
            });

        if (this.reconcilationEnabled) {
            startReconciler();
        }

        return promise.future();
    }

    @Override
    public List<JsonObject> getJwks() {
        return this.jwtAuthOptions.getJwks();
    }

    protected JWTAuthOptions getJWTAuthOptions() {
        return this.jwtAuthOptions;
    }
}
