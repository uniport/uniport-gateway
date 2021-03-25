package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class OAuth2AuthMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddleware.class);

    private OAuth2AuthHandler authHandler;
    private String sessionScope;

    public OAuth2AuthMiddleware(OAuth2AuthHandler authHandler, String sessionScope) {
        this.authHandler = authHandler;
        this.sessionScope = sessionScope;
    }

    @Override
    public void handle(RoutingContext ctx) {
        User sessionScopeUser = ctx.session().get(
                String.format(OAuth2MiddlewareFactory.SESSION_SCOPE_USER_FORMAT, sessionScope));
        ctx.setUser(sessionScopeUser);

        LOGGER.debug("Handling auth request");
        authHandler.handle(ctx);
        LOGGER.debug("Handled auth request");
    }
}
