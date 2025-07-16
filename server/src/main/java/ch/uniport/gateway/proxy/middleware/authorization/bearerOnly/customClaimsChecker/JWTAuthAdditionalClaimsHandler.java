package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker;

import ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.publickeysReconciler.JWTAuthPublicKeysReconcilerHandler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

/**
 */
public interface JWTAuthAdditionalClaimsHandler extends AuthenticationHandler {

    static JWTAuthHandler create(JWTAuth authProvider) {
        return create(authProvider, null, null);
    }

    static JWTAuthHandler create(JWTAuth authProvider, JWTAuthAdditionalClaimsOptions options) {
        return create(authProvider, options, null);
    }

    static JWTAuthHandler create(JWTAuth authProvider, JWTAuthPublicKeysReconcilerHandler reconciler) {
        return create(authProvider, null, reconciler);
    }

    static JWTAuthHandler create(
        JWTAuth authProvider, JWTAuthAdditionalClaimsOptions options,
        JWTAuthPublicKeysReconcilerHandler reconciler
    ) {
        return new JWTAuthAdditionalClaimsHandlerImpl(authProvider, options, reconciler);
    }
}
