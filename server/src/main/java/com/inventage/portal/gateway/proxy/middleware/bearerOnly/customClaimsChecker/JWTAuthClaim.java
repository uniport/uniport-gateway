package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;

/**
In order for our custom jwt claim check to be invoked, we copied and modified some classes of the vertx library.
This class is a copy of its superclass, with the difference that in the create method we return our customized implementation for the jwt verification
 */
public interface JWTAuthClaim extends JWTAuth {

    /**
     * Create a JWT auth provider, call our custom implementation
     *
     * @param vertx the Vertx instance
     * @param config  the config
     * @return the auth provider
     */
    static JWTAuthProviderImpl create(Vertx vertx, JWTAuthOptions config) {
        //Return our custom implementation
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
