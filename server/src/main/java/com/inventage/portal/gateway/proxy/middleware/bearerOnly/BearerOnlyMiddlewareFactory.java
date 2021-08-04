package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.codec.BodyCodec;

public class BearerOnlyMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BEARER_ONLY;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("create: Creating '{}' middleware", DynamicConfiguration.MIDDLEWARE_BEARER_ONLY);
        Promise<Middleware> bearerOnlyPromise = Promise.promise();

        String issuer = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_ISSUER);
        JsonArray audience = middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_AUDIENCE);
        String publicKeyAlgorithm = middlewareConfig
                .getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_ALGORITHM, "RS256");

        this.fetchPublicKey(vertx, middlewareConfig).onSuccess(publicKey -> {
            String publicKeyInPEMFormat = String.join("\n", "-----BEGIN PUBLIC KEY-----", publicKey,
                    "-----END PUBLIC KEY-----");

            JWTOptions jwtOptions = new JWTOptions();
            if (issuer != null) {
                jwtOptions.setIssuer(issuer);
            }
            if (audience != null) {
                jwtOptions.setAudience(audience.getList());
            }
            JWTAuthOptions authConfig = new JWTAuthOptions()
                    .addPubSecKey(
                            new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyInPEMFormat))
                    .setJWTOptions(jwtOptions);

            JWTAuth authProvider = JWTAuth.create(vertx, authConfig);
            AuthenticationHandler authHandler = JWTAuthHandler.create(authProvider);

            bearerOnlyPromise.handle(Future.succeededFuture(new BearerOnlyMiddleware(authHandler)));
            LOGGER.debug("create: Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_BEARER_ONLY);
        }).onFailure(err -> {
            String errMsg = String.format("create: Failed to get public key '%s'", err.getMessage());
            LOGGER.info(errMsg);
            bearerOnlyPromise.handle(Future.failedFuture(errMsg));
        });

        return bearerOnlyPromise.future();
    }

    private Future<String> fetchPublicKey(Vertx vertx, JsonObject middlewareConfig) {
        Promise<String> promise = Promise.promise();
        this.fetchPublicKey(vertx, middlewareConfig, promise);
        return promise.future();
    }

    private void fetchPublicKey(Vertx vertx, JsonObject middlewareConfig, Handler<AsyncResult<String>> handler) {
        // the public key is either base64 encoded OR a valid URL to fetch it from
        String publicKey = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY);

        try {
            Base64.getDecoder().decode(publicKey);
            handler.handle(Future.succeededFuture(publicKey));
            return;
        } catch (IllegalArgumentException e) {
        }

        this.fetchPublicKeyFromURL(vertx, publicKey, handler);
    }

    private void fetchPublicKeyFromURL(Vertx vertx, String rawUrl, Handler<AsyncResult<String>> handler) {
        URL parsedURL;
        try {
            parsedURL = new URL(rawUrl);
        } catch (MalformedURLException e) {
            LOGGER.info("fetchPublicKeyFromURL: Malformed URL '{}'", rawUrl);
            handler.handle(Future.failedFuture(e));
            return;
        }

        String protocol = parsedURL.getProtocol();
        String host = parsedURL.getHost();
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

        LOGGER.debug("fetchPublicKeyFromURL: reading public key from URL '{}://{}:{}{}'", protocol, host, port, path);
        WebClient.create(vertx).get(port, host, path).as(BodyCodec.jsonObject()).send().onSuccess(resp -> {
            JsonObject json = resp.body();

            String publicKey = json.getString("public_key");
            if (publicKey == null || publicKey.length() == 0) {
                String errMsg = "fetchPublicKeyFromURL: No public key found";
                LOGGER.info(errMsg);
                handler.handle(Future.failedFuture(errMsg));
            }

            LOGGER.debug("fetchPublicKeyFromURL: Successfully retrieved public key from URL");
            handler.handle(Future.succeededFuture(publicKey));
        }).onFailure(err -> {
            LOGGER.info("fetchPublicKeyFromURL: Failed to read public key from URL");
            handler.handle(Future.failedFuture(err));
        });
    }
}
