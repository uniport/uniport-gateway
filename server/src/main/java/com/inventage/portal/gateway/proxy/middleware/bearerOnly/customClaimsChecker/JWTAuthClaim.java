package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;

public interface JWTAuthClaim extends JWTAuth {

    /**
     * Create a JWT auth provider, call our custom implementation
     *
     * @param vertx the Vertx instance
     * @param config  the config
     * @return the auth provider
     */
    static JWTAuthProviderImpl create(Vertx vertx, JWTAuthOptions config) {
        return new JWTAuthClaimProviderImpl(vertx, config);
    }
}
