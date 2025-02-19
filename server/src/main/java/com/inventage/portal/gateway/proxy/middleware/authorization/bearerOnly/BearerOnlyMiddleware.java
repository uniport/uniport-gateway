package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BearerOnly middleware checks if incoming requests are authenticated.
 * <p>
 * Per default, incoming requests are required to have a header with 'Authorization: Bearer <token>'.
 * The token has to be a valid JWT matching its signature, expiration, audience and issuer with
 * the properties configured for this middleware.
 * The middleware may be configured to allow requests with no 'Authorization' header to pass through.
 * <p>
 * <p>
 * If no valid token is provided, then a '401 Unauthorized' is returned.
 * Otherwise, the request is forwarded.
 */
public class BearerOnlyMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddleware.class);

    private final String name;
    private final AuthenticationHandler authHandler;
    private final boolean optional;

    public BearerOnlyMiddleware(String name, AuthenticationHandler authHandler, boolean optional) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(authHandler, "authHandler must not be null");

        this.name = name;
        this.authHandler = authHandler;

        this.optional = optional;
        if (optional) {
            LOGGER.info("Requests are not required to carry a 'Authorization' header");
        }
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        final String authorization = ctx.request().headers().get(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            LOGGER.debug("Authentication by '{}'", authorization);
        } else if (optional) {
            LOGGER.debug("Letting through request with no authorization header");
            ctx.next();
            return;
        }
        LOGGER.debug("Handling jwt auth request");
        authHandler.handle(ctx);
        LOGGER.debug("Handled jwt auth request");
    }
}
