package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.publickeysReconciler.JWTAuthPublicKeysReconcilerHandler;
import com.jayway.jsonpath.internal.path.PathCompiler;
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
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract factory for shared WithAuthHandlerMiddleware.
 */
public abstract class WithAuthHandlerMiddlewareFactoryBase implements MiddlewareFactory {

    // schema
    public static final String AUDIENCE = "audience";
    public static final String CLAIMS = "claims";
    public static final String CLAIM_OPERATOR = "operator";
    public static final String CLAIM_OPERATOR_CONTAINS = "CONTAINS";
    public static final String CLAIM_OPERATOR_CONTAINS_SUBSTRING_WHITESPACE = "CONTAINS_SUBSTRING_WHITESPACE";
    public static final String CLAIM_OPERATOR_EQUALS = "EQUALS";
    public static final String CLAIM_OPERATOR_EQUALS_SUBSTRING_WHITESPACE = "EQUALS_SUBSTRING_WHITESPACE";
    public static final String CLAIM_PATH = "claimPath";
    public static final String CLAIM_VALUE = "value";
    public static final String ISSUER = "issuer";
    public static final String ADDITIONAL_ISSUERS = "additionalIssuers";
    public static final String PUBLIC_KEYS = "publicKeys";
    public static final String PUBLIC_KEY = "publicKey";
    public static final String PUBLIC_KEY_ALGORITHM = "publicKeyAlgorithm";
    public static final String PUBLIC_KEYS_RECONCILIATION = "publicKeysReconcilation";
    public static final String RECONCILIATION_ENABLED = "enabled";
    public static final String RECONCILIATION_INTERVAL_MS = "intervalMs";

    public static final List<String> OPERATORS = List.of(
        CLAIM_OPERATOR_CONTAINS,
        CLAIM_OPERATOR_CONTAINS_SUBSTRING_WHITESPACE,
        CLAIM_OPERATOR_EQUALS,
        CLAIM_OPERATOR_EQUALS_SUBSTRING_WHITESPACE);

    private static final Logger LOGGER = LoggerFactory.getLogger(WithAuthHandlerMiddlewareFactoryBase.class);

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        final ArraySchemaBuilder publicKeysSchema = Schemas.arraySchema()
            .with(Keywords.minItems(1))
            .items(Schemas.objectSchema()
                .requiredProperty(PUBLIC_KEY, Schemas.stringSchema()
                    .with(Keywords.minLength(1)))
                .optionalProperty(PUBLIC_KEY_ALGORITHM, Schemas.stringSchema()
                    .with(Keywords.minLength(1))
                    .defaultValue(WithAuthHandlerMiddlewareOptionsBase.DEFAULT_PUBLIC_KEY_ALGORITHM))
                .allowAdditionalProperties(false));

        final ArraySchemaBuilder claimsSchema = Schemas.arraySchema()
            .items(Schemas.objectSchema()
                .requiredProperty(CLAIM_OPERATOR, Schemas.enumSchema(OPERATORS.toArray()))
                .requiredProperty(CLAIM_PATH, Schemas.stringSchema()
                    .with(Keywords.minLength(1)))
                .requiredProperty(CLAIM_VALUE, Schemas.schema()) // TODO be more restrictive here
                .allowAdditionalProperties(false));

        final ObjectSchemaBuilder reconciliationSchema = Schemas.objectSchema()
            .optionalProperty(RECONCILIATION_ENABLED, Schemas.booleanSchema()
                .defaultValue(WithAuthHandlerMiddlewareOptionsBase.DEFAULT_RECONCILIATION_ENABLED_VALUE))
            .optionalProperty(RECONCILIATION_INTERVAL_MS, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(WithAuthHandlerMiddlewareOptionsBase.DEFAULT_RECONCILIATION_INTERVAL_MS))
            .allowAdditionalProperties(false);

        return Schemas.objectSchema()
            .requiredProperty(AUDIENCE, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .requiredProperty(ISSUER, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(PUBLIC_KEYS, publicKeysSchema)
            .optionalProperty(ADDITIONAL_ISSUERS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(CLAIMS, claimsSchema)
            .optionalProperty(PUBLIC_KEYS_RECONCILIATION, reconciliationSchema)
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonArray publicKeys = options.getJsonArray(PUBLIC_KEYS);
        if (publicKeys == null || publicKeys.size() == 0) {
            return Future.failedFuture(new IllegalStateException("No public keys defined"));
        }
        for (int j = 0; j < publicKeys.size(); j++) {
            final JsonObject pk;
            try {
                pk = publicKeys.getJsonObject(j);
            } catch (ClassCastException e) {
                return Future.failedFuture(new IllegalStateException("Invalid publickeys format"));
            }

            final String publicKey = pk.getString(PUBLIC_KEY);
            if (publicKey == null) {
                return Future.failedFuture(new IllegalStateException("No public key defined"));
            } else if (publicKey.length() == 0) {
                return Future.failedFuture(new IllegalStateException("Empty public key defined"));
            }

            // the public key has to be either a valid URL to fetch it from or base64
            // encoded
            boolean isBase64;
            try {
                Base64.getDecoder().decode(publicKey);
                isBase64 = true;
            } catch (IllegalArgumentException e) {
                isBase64 = false;
            }

            boolean isURL = false;
            if (!isBase64) {
                try {
                    new URL(publicKey).toURI();
                    isURL = true;
                } catch (MalformedURLException | URISyntaxException e) {
                    isURL = false;
                }
            }

            if (!isBase64 && !isURL) {
                return Future.failedFuture("Public key is required to either be base64 encoded or a valid URL");
            }

            final String publicKeyAlgorithm = pk.getString(PUBLIC_KEY_ALGORITHM);
            if (isBase64 && publicKeyAlgorithm.length() == 0) {
                // only needed when the public key is given directly
                return Future.failedFuture("No public key algorithm");
            }
        }

        final JsonArray claims = options.getJsonArray(CLAIMS);
        if (claims != null) {
            if (claims.size() == 0) {
                LOGGER.debug("Claims is empty");
            }
            for (Object claim : claims.getList()) {
                if (claim instanceof Map) {
                    claim = new JsonObject((Map<String, Object>) claim);
                }
                if (!(claim instanceof JsonObject)) {
                    return Future.failedFuture("Claim is required to be a JsonObject");
                }

                final JsonObject cObj = (JsonObject) claim;
                if (cObj.size() != 3) {
                    return Future.failedFuture("Claim is required to contain exactly 3 entries. Namely: claimPath, operator and value");
                }

                if (!(cObj.containsKey(CLAIM_PATH)
                    && cObj.containsKey(CLAIM_OPERATOR)
                    && cObj.containsKey(CLAIM_VALUE))) {
                    return Future.failedFuture(String.format(
                        "Claim is missing at least 1 key. Required keys: %s, %s, %s",
                        CLAIM_OPERATOR,
                        CLAIM_PATH,
                        CLAIM_VALUE));
                }

                final String path = cObj.getString(CLAIM_PATH);
                try {
                    PathCompiler.compile(path);
                } catch (RuntimeException e) {
                    final String errMsg = String.format("Invalid claim path %s", path);
                    LOGGER.debug(errMsg);
                    return Future.failedFuture(errMsg);
                }
            }
        }

        return Future.succeededFuture();
    }

    /**
     * Creates the actual middleware.
     *
     * @param authHandler
     *            The {@link AuthenticationHandler} to use in the middleware
     * @param middlewareConfig
     *            The config for the middleware
     * @return Your {@link Middleware}
     */
    protected abstract Middleware create(Vertx vertx, String name, JWKAccessibleAuthHandler authHandler, MiddlewareOptionsModel config);

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final Promise<Middleware> promise = Promise.promise();
        this.create(vertx, name, router, config, promise);
        return promise.future();
    }

    public void create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config, Handler<AsyncResult<Middleware>> handler) {
        LOGGER.debug("Creating '{}' middleware", provides());
        final WithAuthHandlerMiddlewareOptionsBase options = castOptions(config, WithAuthHandlerMiddlewareOptionsBase.class);

        final JWTOptions jwtOptions = new JWTOptions();
        final String issuer = options.getIssuer();
        if (issuer != null) {
            jwtOptions.setIssuer(issuer);
            LOGGER.debug("With issuer '{}'", issuer);
        }

        final List<String> audience = options.getAudience();
        if (audience != null && !audience.isEmpty()) {
            jwtOptions.setAudience(audience);
            LOGGER.debug("With audience '{}'", audience);
        }

        final List<String> additionalIssuers = options.getAdditionalIssuers();
        final JWTAuthMultipleIssuersOptions additionalIssuersOptions = new JWTAuthMultipleIssuersOptions();
        if (additionalIssuers != null && !additionalIssuers.isEmpty()) {
            additionalIssuersOptions.setAdditionalIssuers(additionalIssuers);
            LOGGER.debug("With additional issuers '{}'", additionalIssuers);
        }

        final List<ClaimOptions> additionalClaims = options.getClaims();
        final JWTAuthAdditionalClaimsOptions additionalClaimsOptions = new JWTAuthAdditionalClaimsOptions();
        if (additionalClaims != null && !additionalClaims.isEmpty()) {
            additionalClaimsOptions.addAdditionalClaims(additionalClaims);
            LOGGER.debug("With claims '{}'", additionalClaims);
        }

        final List<PublicKeyOptions> publicKeySources = options.getPublicKeys();
        final ReconciliationOptions publicKeysReconciliation = options.getReconciliation();
        final boolean publicKeysReconciliationEnabled = publicKeysReconciliation.isEnabled();
        final long publicKeysReconciliationIntervalMs = publicKeysReconciliation.getIntervalMs();
        JWTAuthPublicKeysReconcilerHandler.fetchPublicKeys(vertx, publicKeySources)
            .onSuccess(authOpts -> {
                final JWTAuthOptions jwtAuthOptions = new JWTAuthOptions(authOpts).setJWTOptions(jwtOptions);

                final JWTAuthPublicKeysReconcilerHandler reconciler = JWTAuthPublicKeysReconcilerHandler.create(
                    vertx, jwtAuthOptions, additionalIssuersOptions, additionalClaimsOptions, publicKeySources, publicKeysReconciliationEnabled, publicKeysReconciliationIntervalMs);

                handler.handle(Future.succeededFuture(create(vertx, name, reconciler, config)));
            })
            .onFailure(err -> handler.handle(Future.failedFuture(err.getMessage())));

    }
}
