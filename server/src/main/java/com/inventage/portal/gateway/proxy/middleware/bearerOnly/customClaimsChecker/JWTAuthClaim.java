package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;


public interface JWTAuthClaim extends JWTAuth {

    /**
     * Create a JWT auth provider
     *
     * @param vertx the Vertx instance
     * @param config  the config
     * @return the auth provider
     */
    static JWTAuthProviderImpl create(Vertx vertx, JWTAuthOptions config) {
        return new JWTAuthClaimProviderImpl(vertx, config);
    }

    /**
     * Generate a new JWT token.
     *
     * @param claims Json with user defined claims for a list of official claims
     *               @see <a href="http://www.iana.org/assignments/jwt/jwt.xhtml">www.iana.org/assignments/jwt/jwt.xhtml</a>
     * @param options extra options for the generation
     *
     * @return JWT encoded token
     */
    String generateToken(JsonObject claims, JWTOptions options);

    /**
     * Generate a new JWT token.
     *
     * @param claims Json with user defined claims for a list of official claims
     *               @see <a href="http://www.iana.org/assignments/jwt/jwt.xhtml">www.iana.org/assignments/jwt/jwt.xhtml</a>
     *
     * @return JWT encoded token
     */
    String generateToken(JsonObject claims);
}
