package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import com.inventage.portal.gateway.proxy.middleware.HttpResponder;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prevents authentication requests not initiated by the Portal-Gateway by redirecting to a fallback URI.
 * A common case is that the authentication URL
 * (= http://localhost:20000/auth/realms/portal/protocol/openid-connect/auth?state=xyz&redirect_uri=...)
 * was bookmarked by the user.
 *
 * PORTAL-1417
 */
public class PreventForeignInitiatedAuthMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreventForeignInitiatedAuthMiddleware.class);

    private static final String DEFAULT_REDIRECT_URI = "/";

    private final String name;

    private final String fallbackURI;

    public PreventForeignInitiatedAuthMiddleware(String name, String fallbackURI) {
        this.name = name;
        this.fallbackURI = fallbackURI != null ? fallbackURI : DEFAULT_REDIRECT_URI;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (isAuthenticationRequestInitiatedByForeign(ctx)) {
            LOGGER.warn("foreign authentication request detected for '{}' in '{}'", ctx.request().uri(), name);
            HttpResponder.respondWithRedirect(fallbackURI, ctx);
            return;
        }
        ctx.next();
    }

    private boolean isAuthenticationRequestInitiatedByForeign(RoutingContext ctx) {
        if (isAuthenticationRequest(ctx)) {
            return checkForInvalidStateParameter(ctx);
        }
        return false;
    }

    private boolean checkForInvalidStateParameter(RoutingContext ctx) {
        return !OAuth2AuthMiddleware.isStateForPendingAuth(ctx);
    }

    // /auth/realms/portal/protocol/openid-connect/auth?
    private boolean isAuthenticationRequest(RoutingContext ctx) {
        return ctx.request().path().contains("/protocol/openid-connect/auth");
    }
}
