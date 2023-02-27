package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsHandler;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;

import io.vertx.core.AsyncResult;
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
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.AuthenticationHandler;

public class BearerOnlyMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareFactory.class);

    private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";

    private static final String JWKS_URI_KEY = "jwks_uri";

    private static final String JWK_KEYS_KEY = "keys";
    private static final String JWK_KTY_KEY = "kty";
    private static final String JWK_ALG_KEY = "alg";
    private static final String JWK_USE_KEY = "use";
    private static final String JWK_MODULUS_KEY = "n";
    private static final String JWK_EXPONENT_KEY = "e";

    private static final String JWK_USE_SIGNING = "sig";

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BEARER_ONLY;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Creating '{}' middleware", DynamicConfiguration.MIDDLEWARE_BEARER_ONLY);
        final Promise<Middleware> bearerOnlyPromise = Promise.promise();

        final String issuer = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_ISSUER);
        final JsonArray audience = middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_AUDIENCE);
        final JsonArray additionalClaims = middlewareConfig
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIMS);

        final String optionalStr = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_OPTIONAL,
                "false");
        final boolean optional = Boolean.parseBoolean(optionalStr);

        this.fetchPublicKeys(vertx, middlewareConfig).onSuccess(publicKeys -> {
            final JWTOptions jwtOptions = new JWTOptions();
            if (issuer != null) {
                jwtOptions.setIssuer(issuer);
                LOGGER.debug("With issuer '{}'", issuer);
            }
            if (audience != null) {
                jwtOptions.setAudience(audience.getList());
                LOGGER.debug("With audience '{}'", audience);
            }

            final JWTAuthOptions authConfig = new JWTAuthOptions().setJWTOptions(jwtOptions);
            publicKeys.forEach(publicKey -> {
                authConfig.addPubSecKey(publicKey);
            });

            final JWTAuthAdditionalClaimsOptions additionalClaimsOptions = new JWTAuthAdditionalClaimsOptions();

            if (additionalClaims != null) {
                additionalClaimsOptions.setAdditionalClaims(additionalClaims);
                LOGGER.debug("With claims '{}'", additionalClaims);
            }

            final JWTAuth authProvider = JWTAuth.create(vertx, authConfig);
            final AuthenticationHandler authHandler = JWTAuthAdditionalClaimsHandler.create(authProvider,
                    additionalClaimsOptions);

            bearerOnlyPromise.handle(Future.succeededFuture(new BearerOnlyMiddleware(authHandler, optional)));
            LOGGER.debug("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_BEARER_ONLY);
        }).onFailure(err -> {
            final String errMsg = String.format("create: Failed to get public key '%s'", err.getMessage());
            LOGGER.info(errMsg);
            bearerOnlyPromise.handle(Future.failedFuture(errMsg));
        });

        return bearerOnlyPromise.future();
    }

    // TODO do we still need a future here?
    private Future<List<PubSecKeyOptions>> fetchPublicKeys(Vertx vertx, JsonObject middlewareConfig) {
        final Promise<List<PubSecKeyOptions>> promise = Promise.promise();
        this.fetchPublicKeys(vertx, middlewareConfig, promise);
        return promise.future();
    }

    private void fetchPublicKeys(Vertx vertx, JsonObject middlewareConfig,
            Handler<AsyncResult<List<PubSecKeyOptions>>> handler) {

        final List<JsonObject> publicKeys = middlewareConfig
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_PUBLIC_KEYS).getList();

        final List<PubSecKeyOptions> publicKeyOpts = new ArrayList<>();
        publicKeys.forEach(pk -> {
            String publicKey = pk.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY);

            boolean isURL = false;
            try {
                new URL(publicKey).toURI();
                isURL = true;
            } catch (MalformedURLException | URISyntaxException e) {
                LOGGER.debug("URI is malformed: " + e.getMessage());
            }

            if (!isURL) {
                // then the public key is provided directly
                final String publicKeyAlgorithm = pk
                        .getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_ALGORITHM);

                publicKeyOpts.add(
                        new PubSecKeyOptions()
                                .setAlgorithm(publicKeyAlgorithm)
                                .setBuffer(publickeyToPEM(publicKey)));

                LOGGER.info("Public key provided directly");
            } else {
                LOGGER.info("Public key provided by URL");

                this.fetchPublicKeysFromURL(vertx, publicKey)
                        .onSuccess(publicKeyOpts::addAll)
                        .onFailure(null);
            }
        });

        handler.handle(Future.succeededFuture(publicKeyOpts));
    }

    private Future<List<PubSecKeyOptions>> fetchPublicKeysFromURL(Vertx vertx, String rawUrl) {
        final Promise<List<PubSecKeyOptions>> promise = Promise.promise();
        this.fetchPublicKeysFromURL(vertx, rawUrl, promise);
        return promise.future();
    }

    private void fetchPublicKeysFromURL(Vertx vertx, String rawUrl,
            Handler<AsyncResult<List<PubSecKeyOptions>>> handler) {
        final URL parsedURL;
        try {
            parsedURL = new URL(rawUrl);
        } catch (MalformedURLException e) {
            LOGGER.info("Malformed URL '{}'", rawUrl);
            handler.handle(Future.failedFuture(e));
            return;
        }

        final String protocol = parsedURL.getProtocol();
        final String host = parsedURL.getHost();
        int port = parsedURL.getPort();
        if (port <= 0) {
            if (protocol.endsWith("s")) {
                port = 443;
            } else {
                port = 80;
            }
        }
        String path = parsedURL.getPath();
        if (path == null) {
            path = "/";
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // discover jwks_uri
        path = path + OIDC_DISCOVERY_PATH;

        final int iamPort = port;
        final String iamPath = path;
        LOGGER.debug("Reading public key from URL '{}://{}:{}{}'", protocol, host, iamPort, iamPath);

        // TODO split up
        WebClient webclient = WebClient.create(vertx);
        webclient.get(iamPort, host, iamPath).as(BodyCodec.jsonObject()).send()
                .onSuccess(discoveryResp -> {
                    final String JWKsURI = discoveryResp.body().getString(JWKS_URI_KEY);
                    if (JWKsURI == null || JWKsURI.length() == 0) {
                        final String errMsg = "No JWK URI found";
                        LOGGER.info(errMsg);
                        handler.handle(Future.failedFuture(errMsg));
                    }

                    webclient.get(JWKsURI).as(BodyCodec.jsonObject()).send()
                            .onSuccess(JWKsResp -> {
                                final JsonArray keys = JWKsResp.body().getJsonArray(JWK_KEYS_KEY);

                                if (keys == null || keys.size() == 0) {
                                    final String errMsg = "No public key found";
                                    LOGGER.warn(errMsg);
                                    handler.handle(Future.failedFuture(errMsg));
                                    return;
                                }

                                final List<PubSecKeyOptions> publicKeyOpts = new ArrayList<>();
                                for (int i = 0; i < keys.size(); i++) {
                                    JsonObject params = keys.getJsonObject(i);

                                    String ktyValue = params.getString(JWK_KTY_KEY);
                                    // String algValue = params.getString(JWK_ALG_KEY); // TODO see assemblePublicKeyOptions
                                    String useValue = params.getString(JWK_USE_KEY);
                                    String nValue = params.getString(JWK_MODULUS_KEY);
                                    String eValue = params.getString(JWK_EXPONENT_KEY);

                                    publicKeyOpts.add(assemblePublicKeyOptions(ktyValue, nValue, eValue, useValue));
                                }

                                LOGGER.debug("Successfully retrieved public keys from URL");
                                handler.handle(Future.succeededFuture(publicKeyOpts));
                            })
                            .onFailure(err -> {
                                LOGGER.info("Failed to complete load JWK from URL '{}'", JWKsURI);
                                handler.handle(Future.failedFuture(err));
                            });
                })
                .onFailure(err -> {
                    LOGGER.info("Failed to complete discovery from URL '{}://{}:{}{}'", protocol, host, iamPort,
                            iamPath);
                    handler.handle(Future.failedFuture(err));
                });
    }

    // for reference see RFC 7517: JSON Web Key (JWK) - https://datatracker.ietf.org/doc/html/rfc7517
    private PubSecKeyOptions assemblePublicKeyOptions(String ktyValue, String nValue, String eValue, String useValue) {
        // only consider keys used for signing
        if (useValue != JWK_USE_SIGNING) {
            return null;
        }

        // the modulus and the public exponent are base64url encoded and are big-endian (luckly BigInteger expects big-endian)
        final BigInteger n = new BigInteger(Base64.getUrlDecoder().decode(nValue));
        final BigInteger e = new BigInteger(Base64.getUrlDecoder().decode(eValue));

        final PublicKey publicKey = publicKeyParamsToPublicKey(ktyValue, n, e);
        if (publicKey == null) {
            return null;
        }

        return new PubSecKeyOptions()
                .setAlgorithm(publicKey.getAlgorithm()) // TODO do we have to set this or is it detected?
                .setBuffer(publickeyToPEM(new String(publicKey.getEncoded(), StandardCharsets.UTF_8)));
    }

    private PublicKey publicKeyParamsToPublicKey(String keyType, BigInteger n, BigInteger e) {
        final KeySpec keyspec;
        switch (keyType) {
            case "RSA":
                keyspec = new RSAPublicKeySpec(n, e);
                break;
            default:
                keyspec = new RSAPublicKeySpec(n, e);
                return null;
        }

        final PublicKey publicKey;
        try {
            publicKey = KeyFactory.getInstance(keyType).generatePublic(keyspec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            LOGGER.warn("Failed to generate public key from components: '{}'", ex.getMessage());
            return null;
        }
        return publicKey;
    }

    private String publickeyToPEM(String publicKey) {
        return String.join(
                "\n",
                "-----BEGIN PUBLIC KEY-----",
                publicKey,
                "-----END PUBLIC KEY-----");
    }
}
