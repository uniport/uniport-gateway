package com.inventage.portal.gateway.core.middleware.oauth2;

import com.inventage.portal.gateway.core.middleware.Middleware;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class OAuth2AuthMiddleware implements Middleware {

    private OAuth2AuthHandler authHandler;
    private String sessionScope;

    public OAuth2AuthMiddleware(OAuth2AuthHandler authHandler, String sessionScope) {
        this.authHandler = authHandler;
        this.sessionScope = sessionScope;
    }

    @Override
    public void handle(RoutingContext ctx) {
        // TODO/ASK
        final User service_user = ctx.session().get(this.sessionScope);
        // ATTENTION: Set the user. Usually used by auth handlers to inject a User. You will not
        // normally call this method.
        ctx.setUser(service_user);

        authHandler.handle(ctx);

        ctx.next();
    }
}
