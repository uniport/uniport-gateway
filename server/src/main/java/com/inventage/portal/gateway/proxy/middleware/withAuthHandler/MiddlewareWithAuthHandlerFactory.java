package com.inventage.portal.gateway.proxy.middleware.withAuthHandler;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

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

public abstract class MiddlewareWithAuthHandlerFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiddlewareWithAuthHandlerFactory.class);

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Creating '{}' middleware", provides());
        final Promise<Middleware> middlewarePromise = Promise.promise();

        final String issuer = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER);
        final JsonArray audience = middlewareConfig
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE);
        final JsonArray additionalClaims = middlewareConfig
                .getJsonArray(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_CLAIMS);

        final String publicKeyAlgorithm = middlewareConfig
                .getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM, "RS256");

        this.fetchPublicKey(vertx, middlewareConfig).onSuccess(publicKey -> {
            final String publicKeyInPEMFormat = String.join("\n", "-----BEGIN PUBLIC KEY-----", publicKey,
                    "-----END PUBLIC KEY-----");

            final JWTOptions jwtOptions = new JWTOptions();
            if (issuer != null) {
                jwtOptions.setIssuer(issuer);
                LOGGER.debug("With issuer '{}'", issuer);
            }
            if (audience != null) {
                jwtOptions.setAudience(audience.getList());
                LOGGER.debug("With audience '{}'", audience);
            }

            final JWTAuthOptions authConfig = new JWTAuthOptions()
                    .addPubSecKey(
                            new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyInPEMFormat))
                    .setJWTOptions(jwtOptions);

            final JWTAuthAdditionalClaimsOptions additionalClaimsOptions = new JWTAuthAdditionalClaimsOptions();

            if (additionalClaims != null) {
                additionalClaimsOptions.setAdditionalClaims(additionalClaims);
                LOGGER.debug("With claims '{}'", additionalClaims);
            }

            final JWTAuth authProvider = JWTAuth.create(vertx, authConfig);
            final AuthenticationHandler authHandler = JWTAuthAdditionalClaimsHandler.create(authProvider,
                    additionalClaimsOptions);

            middlewarePromise.handle(Future.succeededFuture(create(name, authHandler, middlewareConfig)));
            LOGGER.debug("Created middleware successfully");
        }).onFailure(err -> {
            final String errMsg = String.format("create: Failed to get public key '%s'", err.getMessage());
            LOGGER.info(errMsg);
            middlewarePromise.handle(Future.failedFuture(errMsg));
        });

        return middlewarePromise.future();
    }

    /**
     * Creates the actual middleware.
     * @param authHandler The {@link AuthenticationHandler} to use in the middleware
     * @param middlewareConfig The config for the middleware
     * @return Your {@link Middleware}
     */
    protected abstract Middleware create(String name, AuthenticationHandler authHandler, JsonObject middlewareConfig);

    private Future<String> fetchPublicKey(Vertx vertx, JsonObject middlewareConfig) {
        final Promise<String> promise = Promise.promise();
        this.fetchPublicKey(vertx, middlewareConfig, promise);
        return promise.future();
    }

    private void fetchPublicKey(Vertx vertx, JsonObject middlewareConfig, Handler<AsyncResult<String>> handler) {
        // the public key is either base64 encoded OR a valid URL to fetch it from
        final String publicKey = middlewareConfig
                .getString(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY);

        boolean isURL = false;
        try {
            new URL(publicKey).toURI();
            isURL = true;
        } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.debug("URI is malformed: " + e.getMessage());
        }

        if (!isURL) {
            handler.handle(Future.succeededFuture(publicKey));
            LOGGER.info("Public key provided directly");
            return;
        }

        LOGGER.info("Public key provided by URL");
        this.fetchPublicKeyFromURL(vertx, publicKey, handler);
    }

    private void fetchPublicKeyFromURL(Vertx vertx, String rawUrl, Handler<AsyncResult<String>> handler) {
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
        if (path == null || path.length() == 0) {
            path = "/";
        }

        final int iamPort = port;
        final String iamPath = path;
        LOGGER.debug("Reading public key from URL '{}://{}:{}{}'", protocol, host, iamPort, iamPath);
        WebClient.create(vertx).get(iamPort, host, iamPath).as(BodyCodec.jsonObject()).send().onSuccess(resp -> {
            final JsonObject json = resp.body();

            final String publicKey = json.getString("public_key");
            if (publicKey == null || publicKey.length() == 0) {
                final String errMsg = "fetchPublicKeyFromURL: No public key found";
                LOGGER.info(errMsg);
                handler.handle(Future.failedFuture(errMsg));
            }

            LOGGER.debug("Successfully retrieved public key from URL");
            handler.handle(Future.succeededFuture(publicKey));
        }).onFailure(err -> {
            LOGGER.info("Failed to read public key from URL '{}://{}:{}{}'", protocol, host, iamPort, iamPath);
            handler.handle(Future.failedFuture(err));
        });
    }
}
