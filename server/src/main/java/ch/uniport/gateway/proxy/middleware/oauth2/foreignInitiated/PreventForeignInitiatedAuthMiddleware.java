package ch.uniport.gateway.proxy.middleware.oauth2.foreignInitiated;

import ch.uniport.gateway.proxy.middleware.HttpResponder;
import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import ch.uniport.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prevents authentication requests not initiated by the Uniport-Gateway by
 * redirecting to a fallback URI.
 * A common case is that the authentication URL
 * (=
 * http://localhost:20000/auth/realms/portal/protocol/openid-connect/auth?state=xyz&redirect_uri=...)
 * was bookmarked by the user.
 *
 * PORTAL-1417
 */
public class PreventForeignInitiatedAuthMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreventForeignInitiatedAuthMiddleware.class);

    private final String name;

    private final String redirectURI;

    public PreventForeignInitiatedAuthMiddleware(String name, String redirectURI) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(redirectURI, "redirectURI must not be null");

        this.name = name;
        this.redirectURI = redirectURI;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        if (isAuthenticationRequestInitiatedByForeign(ctx)) {
            LOGGER.warn("foreign authentication request detected for '{}' in '{}'", ctx.request().uri(), name);
            HttpResponder.respondWithRedirect(redirectURI, ctx);
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
