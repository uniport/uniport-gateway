package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class OAuth2AuthMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddleware.class);

    private OAuth2AuthHandler authHandler;

    public OAuth2AuthMiddleware(OAuth2AuthHandler authHandler) {
        this.authHandler = authHandler;
    }


    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("Handling auth request");
        authHandler.handle(ctx);
        LOGGER.debug("Handled auth request");
    }
}
