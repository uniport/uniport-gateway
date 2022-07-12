package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;

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
 * Otherwise, the request is forwarded.
 */
public class BearerOnlyMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddleware.class);

    private final AuthenticationHandler authHandler;
    private final boolean optional;

    public BearerOnlyMiddleware(AuthenticationHandler authHandler, boolean optional) {
        this.authHandler = authHandler;

        this.optional = optional;
        if (optional) {
            LOGGER.info("Requests are not required to carry a 'Authorization' header");
        }

    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("'{}'", ctx.request().absoluteURI());

        final String authorization = ctx.request().headers().get(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            LOGGER.debug("authentication by '{}'", authorization);
        } else if (optional) {
            LOGGER.debug("letting through request with no authorization header");
            ctx.next();
            return;
        }
        LOGGER.debug("Handling jwt auth request");
        authHandler.handle(ctx);
        LOGGER.debug("Handled jwt auth request");
    }

}
