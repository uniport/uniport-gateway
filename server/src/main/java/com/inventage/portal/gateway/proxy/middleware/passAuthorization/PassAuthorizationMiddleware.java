package com.inventage.portal.gateway.proxy.middleware.passAuthorization;

import com.inventage.portal.gateway.proxy.middleware.withAuthToken.MiddlewareWithAuthToken;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the internal token to make sure the user is allowed to access the backend.
 * It then adds the token that came with the original request to the backend.
 */
public class PassAuthorizationMiddleware extends MiddlewareWithAuthToken {

    public static final String BEARER = "Bearer ";

    private static final Logger LOGGER = LoggerFactory.getLogger(PassAuthorizationMiddleware.class);

    private final AuthenticationHandler authHandler;

    public PassAuthorizationMiddleware(String sessionScope, AuthenticationHandler authHandler) {
        super(sessionScope);
        this.authHandler = authHandler;
    }

    @Override
    public void handle(RoutingContext ctx) {
        this.getAuthToken(ctx.session()).onSuccess(token -> {
            LOGGER.debug("authToken: " + token);

            final String incomingAuthHeader = getAndReplaceAuthHeader(ctx, BEARER + token);

            LOGGER.debug("incomingAuthHeader: " + incomingAuthHeader);

            LOGGER.debug("Handling jwt auth request");
            authHandler.handle(ctx);
            LOGGER.debug("Handled jwt auth request");

            getAndReplaceAuthHeader(ctx, incomingAuthHeader);

            ctx.next();
        }).onFailure(err -> {
            LOGGER.debug("Failed to get token '{}'", err.getMessage());
            ctx.fail(401, err);
        });
    }

    private String getAndReplaceAuthHeader(RoutingContext ctx, String newHeader) {
        final String originalHeader = ctx.request().getHeader(HttpHeaders.AUTHORIZATION);

        ctx.request().headers().remove(HttpHeaders.AUTHORIZATION);
        if (newHeader != null) {
            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, newHeader);
        }

        return originalHeader;
    }


}
