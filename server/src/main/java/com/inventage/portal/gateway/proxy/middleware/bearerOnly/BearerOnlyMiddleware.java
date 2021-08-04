package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;

public class BearerOnlyMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddleware.class);

    private AuthenticationHandler authHandler;
    private boolean optional;

    public BearerOnlyMiddleware(AuthenticationHandler authHandler, boolean optional) {
        this.authHandler = authHandler;
        this.optional = optional;
        if (optional) {
            LOGGER.info("constructor: Requests are not required to carry a 'Authorization' header");
        }
    }

    /**
     * The BearerOnly middleware checks if incoming requests are authenticated.
     *
     * Per default, incoming requests are required to have a header with 'Authorization: Bearer <token>'.
     * The token has to be a valid JWT matching its signature, expiration, audience and issuer with
     * the properties configured for this middleware.
     * The middleware may be configured to allow requests with no 'Authorization' header to pass through.
     *
     *
     * If no valid token is provided, then a '401 Unauthorized' is returned.
     * Otherwise, the request is forwared.
     */
    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("handle: '{}'", ctx.request().absoluteURI());

        if (!ctx.request().headers().contains(HttpHeaders.AUTHORIZATION) && optional) {
            LOGGER.debug("handle: letting through request with no authorization header");
            ctx.next();
            return;
        }

        LOGGER.debug("handle: Handling jwt auth request");
        authHandler.handle(ctx);
        LOGGER.debug("handle: Handled jwt auth request");

    }

}
