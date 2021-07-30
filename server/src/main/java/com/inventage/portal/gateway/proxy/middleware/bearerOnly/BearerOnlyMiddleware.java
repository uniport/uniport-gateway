package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;

public class BearerOnlyMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddleware.class);

    private AuthenticationHandler authHandler;

    public BearerOnlyMiddleware(AuthenticationHandler authHandler) {
        this.authHandler = authHandler;
    }

    /**
     * The BearerOnly middleware checks if incoming requests are authenticated and authorized.
     *
     * Incoming requests are required to have a header with 'Authorization: Bearer <token>'.
     * The token has to be a valid JWT matching its signature, expiration, audience and issuer with
     * the properties configured for this middleware.
     *
     * If no valid token is provided, then a '401 Unauthorized' is returned.
     * Otherwise, the request is forwared.
     */
    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("handle: '{}'", ctx.request().absoluteURI());

        LOGGER.debug("handle: Handling jwt auth request");
        authHandler.handle(ctx);
        LOGGER.debug("handle: Handled jwt auth request");

    }

}
