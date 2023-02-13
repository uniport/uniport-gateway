package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import com.inventage.portal.gateway.proxy.middleware.withAuthToken.MiddlewareWithAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages the authentication bearer. If the user is authenticated it provides either an ID token or
 * an access token as defined in the sessionScope. Access tokens are only provided if the
 * sessionScope matches the corresponding scope of the OAuth2 provider. It also ensures that no
 * token is sent to the Client.
 */
public class AuthorizationBearerMiddleware extends MiddlewareWithAuthToken {

    public static final String BEARER = "Bearer ";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddleware.class);

    public AuthorizationBearerMiddleware(String sessionScope) {
        super(sessionScope);
    }

    @Override
    public void handle(RoutingContext ctx) {
        this.getAuthToken(ctx.session()).onSuccess(token -> {
            if (token == null || token.length() == 0) {
                LOGGER.debug("Skipping empty token");
                ctx.next();
                return;
            }

            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, BEARER + token);

            final Handler<MultiMap> respHeadersModifier = headers -> {
                headers.remove(HttpHeaders.AUTHORIZATION);
            };
            this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);

            ctx.next();
        }).onFailure(err -> {
            LOGGER.debug("Providing no token '{}'", err.getMessage());
            ctx.next();
        });
    }

}
