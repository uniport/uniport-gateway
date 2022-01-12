package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for handling parallel OAuth2 flows for a Vert.x session.
 * The authentication can be required for the same scope (scope = "OIDC client id") of for different scopes.
 * Redirects the user if not authenticated.
 */
public class OAuth2AuthMiddleware implements Middleware {

    public static final String PREFIX_STATE = "state_";
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddleware.class);
    private static final String PENDING_AUTH = "pendingAuth";

    private AuthenticationHandler authHandler;
    private String sessionScope;

    public OAuth2AuthMiddleware(AuthenticationHandler authHandler, String sessionScope) {
        LOGGER.debug("constructor: for session scope '{}'", sessionScope);
        this.authHandler = authHandler;
        this.sessionScope = sessionScope;
    }

    /**
     *
     * @param ctx
     */
    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("handle: uri '{}'", ctx.request().uri());
        final User user = setUserForScope(this.sessionScope, ctx);

        if (user == null && pendingAuth(ctx)) {
            waitForPendingAuth(ctx).onComplete(event -> {
                retryByRedirect(ctx);
            });
        }
        else {
            authHandler.handle(ctx);
            startAndStorePendingAuth(ctx);
            LOGGER.debug("handle: done for uri '{}'", ctx.request().uri());
        }
    }

    /**
     *
     * @param ctx
     * @return a future for the pending authentication
     */
    private Future<Object> waitForPendingAuth(RoutingContext ctx) {
        LOGGER.debug("waitForPendingAuth");

        final Object pendingAuth = ctx.session().get(PENDING_AUTH);
        if (pendingAuth instanceof Promise) {
            LOGGER.debug("waitForPendingAuth: promise='{}'", pendingAuth.hashCode());
            final Future pendingAuthFuture = ((Promise) pendingAuth).future();
            return pendingAuthFuture;
        }
        else {
            LOGGER.debug("waitForPendingAuth: no promise found in session");
            final Promise<Object> promise = Promise.promise();
            promise.complete();
            return promise.future();
        }
    }

    private void retryByRedirect(RoutingContext ctx) {
        LOGGER.debug("retryByRedirect: to same uri '{}'", ctx.request().uri());
        ctx.redirect(ctx.request().uri());
    }

    private User setUserForScope(String sessionScope, RoutingContext ctx) {
        String key = String.format("%s%s", sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
        Pair<OAuth2Auth, User> authPair = ctx.session().get(key);
        if (authPair != null) {
            ctx.setUser(authPair.getRight());
            return authPair.getRight();
        } else {
            ctx.setUser(null);
            return null;
        }
    }

    /**
     * Create a promise if there is a "state" key in the session data.
     * @param ctx
     */
    private void startAndStorePendingAuth(RoutingContext ctx) {
        final Object state = ctx.session().get("state");
        if (state != null) {
            ctx.session().put(PENDING_AUTH, Promise.promise());
            ctx.session().put(PREFIX_STATE+state, state);
            LOGGER.debug("startAndStorePendingAuth: for scope '{}'", sessionScope);
        }
    }

    /**
     * is an auth flow running for this session
     * @param ctx
     * @return
     */
    private boolean pendingAuth(RoutingContext ctx) {
        final Object pendingAuth = ctx.session().get(PENDING_AUTH);
        return pendingAuth != null;
    }

    /**
     * Complete and remove the 'PENDING_AUTH' promise if it is stored in the session.
     * @param ctx
     * @param sessionScope
     */
    public static void endAndRemovePendingAuth(RoutingContext ctx, String sessionScope) {
        final Object pendingAuth = ctx.session().get(PENDING_AUTH);
        if (pendingAuth instanceof Promise) {
            LOGGER.debug("endPendingAuth: for scope '{}'", sessionScope);
            ctx.session().remove(PENDING_AUTH);
            ((Promise)pendingAuth).complete();
        }
    }

    public static void restoreStateParameterFromRequest(RoutingContext ctx, String sessionScope) {
        final String requestState = ctx.request().getParam("state");
        if (requestState != null) {
            final Object state = ctx.session().get(PREFIX_STATE+requestState);
            if (state != null) {
                ctx.session().put("state", state);
                LOGGER.debug("restoreStateParameterFromRequest: for scope '{}'", sessionScope);
            }
        }
    }

}
