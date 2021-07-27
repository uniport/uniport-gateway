package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

/**
 * Redirects the user if not authenticated.
 */
public class OAuth2AuthMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddleware.class);

    private OAuth2AuthHandler authHandler;
    private String sessionScope;

    public OAuth2AuthMiddleware(OAuth2AuthHandler authHandler, String sessionScope) {
        LOGGER.debug("constructor: for session scope '{}'", sessionScope);
        this.authHandler = authHandler;
        this.sessionScope = sessionScope;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("handle: '{}'", ctx.request().absoluteURI());
        String key = String.format("%s%s", this.sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
        Pair<OAuth2Auth, User> authPair = ctx.session().get(key);
        User sessionScopeUser = authPair.getRight();

        ctx.setUser(sessionScopeUser);

        LOGGER.debug("handle: Handling auth request");
        authHandler.handle(ctx);
        LOGGER.debug("handle: Handled auth request");
    }
}
