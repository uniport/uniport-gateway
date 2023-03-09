package com.inventage.portal.gateway.proxy.middleware.authorization;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.AuthenticationHandler;

public abstract class WithAuthHandlerMiddlewareFactoryBase implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(WithAuthHandlerMiddlewareFactoryBase.class);

    private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";

    private static final String JWKS_URI_KEY = "jwks_uri";
    private static final String JWK_KEYS_KEY = "keys";

    /**
     * Creates the actual middleware.
     *
     * @param authHandler      The {@link AuthenticationHandler} to use in the middleware
     * @param middlewareConfig The config for the middleware
     * @return Your {@link Middleware}
     */
    protected abstract Middleware create(String name, AuthenticationHandler authHandler, JsonObject middlewareConfig);

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Creating '{}' middleware", provides());
        final Promise<Middleware> middlewarePromise = Promise.promise();

        final String issuer = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER);
        final JsonArray audience = middlewareConfig
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE);
        final JsonArray additionalClaims = middlewareConfig
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_CLAIMS);
        final JsonArray publicKeys = middlewareConfig
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEYS);

        this.fetchPublicKeys(vertx, publicKeys)
                .onSuccess(setupMiddleware(vertx, name,
                        issuer, audience, additionalClaims,
                        middlewareConfig, middlewarePromise))
                .onFailure(err -> {
                    final String errMsg = String.format("create: Failed to get public key '%s'", err.getMessage());
                    LOGGER.warn(errMsg);
                    middlewarePromise.handle(Future.failedFuture(errMsg));
                });

        return middlewarePromise.future();
    }

    private Handler<JWTAuthOptions> setupMiddleware(Vertx vertx, String name, String issuer, JsonArray audience,
            JsonArray additionalClaims, JsonObject middlewareConfig,
            Handler<AsyncResult<Middleware>> handler) {
        return authOpts -> {
            final JWTOptions jwtOptions = new JWTOptions();
            if (issuer != null) {
                jwtOptions.setIssuer(issuer);
                LOGGER.debug("With issuer '{}'", issuer);
            }
            if (audience != null) {
                jwtOptions.setAudience(audience.getList());
                LOGGER.debug("With audience '{}'", audience);
            }

            final JWTAuthOptions authConfig = new JWTAuthOptions(authOpts).setJWTOptions(jwtOptions);

            final JWTAuthAdditionalClaimsOptions additionalClaimsOptions = new JWTAuthAdditionalClaimsOptions();

            if (additionalClaims != null) {
                additionalClaimsOptions.setAdditionalClaims(additionalClaims);
                LOGGER.debug("With claims '{}'", additionalClaims);
            }

            final JWTAuth authProvider = JWTAuth.create(vertx, authConfig);
            final AuthenticationHandler authHandler = JWTAuthAdditionalClaimsHandler.create(authProvider,
                    additionalClaimsOptions);

            handler.handle(Future.succeededFuture(create(name, authHandler, middlewareConfig)));
            LOGGER.debug("Created middleware successfully");
        };
    }

    private Future<JWTAuthOptions> fetchPublicKeys(Vertx vertx, JsonArray publicKeys) {
        final Promise<JWTAuthOptions> promise = Promise.promise();
        this.fetchPublicKeys(vertx, publicKeys, promise);
        return promise.future();
    }

    private void fetchPublicKeys(Vertx vertx, JsonArray rawPublicKeys,
            Handler<AsyncResult<JWTAuthOptions>> handler) {

        final List<JsonObject> publicKeys = rawPublicKeys.getList();

        final JWTAuthOptions authOpts = new JWTAuthOptions();
        final List<Future> futures = new LinkedList<>();

        publicKeys.forEach(pk -> {
            final String publicKey = pk.getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY);
            boolean isURL = false;
            try {
                new URL(publicKey).toURI();
                isURL = true;
            } catch (MalformedURLException | URISyntaxException e) {
                // intended case
                LOGGER.debug("URI is malformed, hence it is has to be a raw public key: '{}'", e.getMessage());
            }

            if (isURL) {
                LOGGER.info("Public key provided by URL. Fetching JWKs...");

                futures.add(
                        this.fetchJWKsFromDiscoveryURL(vertx, publicKey)
                                .onSuccess(jwks -> {
                                    jwks.forEach(jwk -> authOpts.addJwk(jwk));
                                })
                                .onFailure(err -> handler.handle(Future.failedFuture(err))));

            } else {
                LOGGER.info("Public key provided directly");

                final String publicKeyAlgorithm = pk
                        .getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM, "RS256");
                authOpts.addPubSecKey(
                        new PubSecKeyOptions()
                                .setAlgorithm(publicKeyAlgorithm)
                                .setBuffer(publickeyToPEM(publicKey)));
            }
        });

        CompositeFuture.join(futures)
                .onSuccess(psk -> {
                    LOGGER.info("Successfully fetched JWKs");
                    handler.handle(Future.succeededFuture(authOpts));
                }).onFailure(err -> {
                    LOGGER.error(err.getMessage());
                    handler.handle(Future.failedFuture(err));
                });
    }

    private Future<List<JsonObject>> fetchJWKsFromDiscoveryURL(Vertx vertx, String rawRealmBaseURL) {
        final Promise<List<JsonObject>> promise = Promise.promise();
        this.fetchJWKsFromDiscoveryURL(vertx, rawRealmBaseURL, promise);
        return promise.future();
    }

    private void fetchJWKsFromDiscoveryURL(Vertx vertx, String rawRealmBaseURL,
            Handler<AsyncResult<List<JsonObject>>> handler) {
        final URL parsedRealmBaseURL;
        try {
            parsedRealmBaseURL = new URL(rawRealmBaseURL);
        } catch (MalformedURLException e) {
            LOGGER.warn("Malformed discovery URL '{}'", rawRealmBaseURL);
            handler.handle(Future.failedFuture(e));
            return;
        }

        final String iamProtocol = parsedRealmBaseURL.getProtocol();
        final String iamHost = parsedRealmBaseURL.getHost();
        int port = parsedRealmBaseURL.getPort();
        if (port <= 0) {
            if (iamProtocol.endsWith("s")) {
                port = 443;
            } else {
                port = 80;
            }
        }

        String path = parsedRealmBaseURL.getPath();
        if (path == null) {
            path = "/";
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // allow both to avoid confusion:
        // * url with OIDC discovery path already included
        // * url with OIDC discovery path not yet included
        if (!path.endsWith(OIDC_DISCOVERY_PATH)) {
            path = path + OIDC_DISCOVERY_PATH;
        }

        // discover jwks_uri
        final int iamPort = port;
        final String iamDiscoveryPath = path;
        LOGGER.debug("Fetching jwks_uri from URL '{}://{}:{}{}'", iamProtocol, iamHost, iamPort, iamDiscoveryPath);
        WebClient.create(vertx).get(iamPort, iamHost, iamDiscoveryPath).as(BodyCodec.jsonObject()).send()
                .onSuccess(fetchJWKsFromJWKsURL(vertx, iamHost, iamPort, handler))
                .onFailure(err -> {
                    LOGGER.info("Failed to complete discovery from URL '{}://{}:{}{}'",
                            iamProtocol, iamHost, iamPort, iamDiscoveryPath);
                    handler.handle(Future.failedFuture(err));
                });
    }

    private Handler<HttpResponse<JsonObject>> fetchJWKsFromJWKsURL(Vertx vertx, String iamHost,
            int iamPort, Handler<AsyncResult<List<JsonObject>>> handler) {
        return discoveryResp -> {
            final String rawJWKsURI = discoveryResp.body().getString(JWKS_URI_KEY);
            if (rawJWKsURI == null || rawJWKsURI.length() == 0) {
                final String errMsg = "No JWK URI found";
                LOGGER.warn(errMsg);
                handler.handle(Future.failedFuture(errMsg));
                return;
            }

            final URL parsedJWKsURL;
            try {
                parsedJWKsURL = new URL(rawJWKsURI);
            } catch (MalformedURLException e) {
                LOGGER.warn("Malformed JWKs URL '{}'", rawJWKsURI);
                handler.handle(Future.failedFuture(e));
                return;
            }

            final String iamJWKsPath = parsedJWKsURL.getPath();
            if (iamJWKsPath == null) {
                final String errMsg = "Failed to discover JWKs URI";
                LOGGER.warn(errMsg);
                handler.handle(Future.failedFuture(errMsg));
                return;
            }

            LOGGER.debug("Fetching JWKS from URL '{}'", rawJWKsURI);
            WebClient.create(vertx).get(iamPort, iamHost, iamJWKsPath).as(BodyCodec.jsonObject()).send()
                    .onSuccess(parseJWKs(handler))
                    .onFailure(err -> {
                        LOGGER.info("Failed to complete load JWK from URL '{}'", rawJWKsURI);
                        handler.handle(Future.failedFuture(err));
                    });
        };
    }

    private Handler<HttpResponse<JsonObject>> parseJWKs(
            Handler<AsyncResult<List<JsonObject>>> handler) {
        return JWKsResp -> {
            LOGGER.debug("Received JWKS");
            final JsonArray keys = JWKsResp.body().getJsonArray(JWK_KEYS_KEY);
            if (keys == null || keys.size() == 0) {
                final String errMsg = "No JWK found";
                LOGGER.warn(errMsg);
                handler.handle(Future.failedFuture(errMsg));
                return;
            }

            final List<JsonObject> jwks = new ArrayList<JsonObject>();
            for (int i = 0; i < keys.size(); i++) {
                jwks.add(keys.getJsonObject(i));
            }

            LOGGER.debug("Successfully fetched {} JWKS from URL", keys.size());
            handler.handle(Future.succeededFuture(jwks));
        };
    }

    private String publickeyToPEM(String publicKey) {
        return String.join(
                "\n",
                "-----BEGIN PUBLIC KEY-----",
                publicKey,
                "-----END PUBLIC KEY-----");
    }
}
