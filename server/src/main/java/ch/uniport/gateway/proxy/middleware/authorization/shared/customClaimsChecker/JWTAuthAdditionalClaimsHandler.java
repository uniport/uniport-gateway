package ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker;

import ch.uniport.gateway.proxy.middleware.authorization.shared.publickeysReconciler.JWTAuthPublicKeysReconcilerHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

/**
 */
public interface JWTAuthAdditionalClaimsHandler extends AuthenticationHandler {

    static JWTAuthHandler create(Vertx vertx, JWTAuth authProvider) {
        return create(vertx, authProvider, null, null);
    }

    static JWTAuthHandler create(Vertx vertx, JWTAuth authProvider, JWTAuthAdditionalClaimsOptions options) {
        return create(vertx, authProvider, options, null);
    }

    static JWTAuthHandler create(Vertx vertx, JWTAuth authProvider, JWTAuthPublicKeysReconcilerHandler reconciler) {
        return create(vertx, authProvider, null, reconciler);
    }

    static JWTAuthHandler create(
        Vertx vertx,
        JWTAuth authProvider, JWTAuthAdditionalClaimsOptions options,
        JWTAuthPublicKeysReconcilerHandler reconciler
    ) {
        return new JWTAuthAdditionalClaimsHandlerImpl(vertx, authProvider, options, reconciler);
    }
}
