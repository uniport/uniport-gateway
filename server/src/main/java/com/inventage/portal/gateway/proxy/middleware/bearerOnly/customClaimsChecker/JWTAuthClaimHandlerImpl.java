package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;


public class JWTAuthClaimHandlerImpl extends HTTPAuthClaimHandler<JWTAuth> implements JWTAuthHandler {


    public JWTAuthClaimHandlerImpl(JWTAuth authProvider) {
        super(authProvider, Type.BEARER);
    }

    @Override
    public void parseCredentials(RoutingContext context, Handler<AsyncResult<Credentials>> handler) {

        parseAuthorization(context, false, parseAuthorization -> {
            if (parseAuthorization.failed()) {
                handler.handle(Future.failedFuture(parseAuthorization.cause()));
                return;
            }

            handler.handle(Future.succeededFuture(new TokenCredentials(parseAuthorization.result())));
        });
    }
}
