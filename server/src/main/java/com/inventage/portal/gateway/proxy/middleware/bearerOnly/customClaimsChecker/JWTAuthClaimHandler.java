package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

public interface JWTAuthClaimHandler extends AuthenticationHandler {

    static JWTAuthHandler create(JWTAuth authProvider){
        return new JWTAuthClaimHandlerImpl(authProvider);
    }
}
