package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

/**
 */
public interface JWTAuthMultipleIssuersProvider extends AuthenticationProvider {

    static JWTAuth create(Vertx vertx, JWTAuthOptions config) {
        return create(vertx, config, null);
    }

    /**
     */
    static JWTAuth create(Vertx vertx, JWTAuthOptions config, JWTAuthMultipleIssuersOptions additionalIssuersOptions) {
        return new JWTAuthMultipleIssuersProviderImpl(vertx, config, additionalIssuersOptions);
    }
}
