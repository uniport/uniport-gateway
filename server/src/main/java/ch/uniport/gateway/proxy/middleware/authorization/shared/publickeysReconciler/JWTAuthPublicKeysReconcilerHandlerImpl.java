package ch.uniport.gateway.proxy.middleware.authorization.shared.publickeysReconciler;

import ch.uniport.gateway.proxy.middleware.authorization.PublicKeyOptions;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker.JWTAuthAdditionalClaimsHandler;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customIssuerChecker.JWTAuthMultipleIssuersProvider;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
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
 * The public key used to verify the JWT siganture is fetched periodically, if
 * enabled.
 */
public class JWTAuthPublicKeysReconcilerHandlerImpl implements JWTAuthPublicKeysReconcilerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthPublicKeysReconcilerHandlerImpl.class);

    private final Vertx vertx;
    private final List<PublicKeyOptions> publicKeySources;
    private final boolean reconciliationEnabled;
    private final long reconciliationIntervalMs;

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
        List<PublicKeyOptions> publicKeySources,
        boolean reconciliationEnabled,
        long reconciliationIntervalMs
    ) {
        this.vertx = vertx;
        this.jwtAuthOptions = jwtAuthOptions;
        this.jwtOptions = jwtAuthOptions.getJWTOptions();
        this.additionalIssuersOptions = additionalIssuersOptions;
        this.additionalClaimsOptions = additionalClaimsOptions;

        this.publicKeySources = publicKeySources;
        this.reconciliationEnabled = reconciliationEnabled;
        this.reconciliationIntervalMs = reconciliationIntervalMs;

        this.authHandler = createAuthHandlerWithFreshPublicKeys(jwtAuthOptions);
        if (this.reconciliationEnabled) {
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
        final JWTAuth authProvider = JWTAuthMultipleIssuersProvider.create(this.vertx, authOptions,
            this.additionalIssuersOptions);
        final AuthenticationHandler authHandler = JWTAuthAdditionalClaimsHandler.create(authProvider,
            additionalClaimsOptions, this);

        return authHandler;
    }

    private void startReconciler() {
        if (this.reconcilerStarted) {
            return;
        }

        LOGGER.info("Starting public keys reconciler loop");
        this.timerId = this.vertx.setPeriodic(this.reconciliationIntervalMs, tId -> {
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

        if (this.reconciliationEnabled) {
            startReconciler();
        }

        return JWTAuthPublicKeysReconcilerHandler.fetchPublicKeys(this.vertx, this.publicKeySources)
            .onFailure(err -> LOGGER.warn(String.format("Failed to refresh public keys '%s'", err.getMessage())))
            .map(authOptions -> {
                this.jwtAuthOptions = authOptions;
                this.authHandler = createAuthHandlerWithFreshPublicKeys(authOptions);
                LOGGER.debug("Refreshed public keys");
                return this;
            });
    }

    @Override
    public List<JsonObject> getJwks() {
        return this.jwtAuthOptions.getJwks();
    }

    protected JWTAuthOptions getJWTAuthOptions() {
        return this.jwtAuthOptions;
    }
}
