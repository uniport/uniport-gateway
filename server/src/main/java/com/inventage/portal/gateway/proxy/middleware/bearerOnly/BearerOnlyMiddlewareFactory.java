package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class BearerOnlyMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_BEARER_ONLY;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("create: Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_BEARER_ONLY);

        String publicKey = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY);
        String publicKeyAlgorithm = middlewareConfig
                .getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_ALGORITHM, "RS256");
        String issuer = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_ISSUER);
        JsonArray audience = middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_AUDIENCE);

        JWTOptions jwtOptions = new JWTOptions().setIssuer(issuer).setAudience(audience.getList());

        JWTAuthOptions authConfig = new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKey))
                .setJWTOptions(jwtOptions);

        JWTAuth authProvider = JWTAuth.create(vertx, authConfig);

        AuthenticationHandler authHandler = JWTAuthHandler.create(authProvider);

        return Future.succeededFuture(new BearerOnlyMiddleware(authHandler));
    }
}
