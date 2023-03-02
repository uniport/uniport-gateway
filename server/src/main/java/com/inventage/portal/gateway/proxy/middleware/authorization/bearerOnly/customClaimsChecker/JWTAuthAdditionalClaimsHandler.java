package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

public interface JWTAuthAdditionalClaimsHandler extends AuthenticationHandler {

    static JWTAuthHandler create(JWTAuth authProvider) {
        return create(authProvider, null);
    }

    static JWTAuthHandler create(JWTAuth authProvider, JWTAuthAdditionalClaimsOptions options) {
        return new JWTAuthAdditionalClaimsHandlerImpl(authProvider, options);
    }
}
