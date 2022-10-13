package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

/**
 * In order for our custom jwt claim check to be invoked, we copied and modified some classes of the vertx library.
 * This class is a copy of its superclass, with the difference that in the create method we return our customized implementation for the jwt verification
 */
public interface JWTAuthClaimHandler extends AuthenticationHandler {


    static JWTAuthHandler create(JWTAuth authProvider) {
        return new JWTAuthClaimHandlerImpl(authProvider);
    }
}
