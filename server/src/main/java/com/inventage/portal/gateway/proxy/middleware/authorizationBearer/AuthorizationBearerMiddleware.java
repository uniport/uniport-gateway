package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages the authentication bearer. If the use is authenticated it provides either an ID token or
 * a access token as defined in the sessionScope. Access tokens are only provided if the
 * sessionScope matches the corresponding scope of the OAuth2 provider. It also ensures that no
 * token is sent to the Client.
 */
public class AuthorizationBearerMiddleware implements Middleware {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthorizationBearerMiddleware.class);

    private final static String BEARER = "Bearer ";

    private String sessionScope;

    public AuthorizationBearerMiddleware(String sessionScope) {
        this.sessionScope = sessionScope;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String idToken = ctx.session().get(OAuth2MiddlewareFactory.ID_TOKEN);
        String accessToken = ctx.session().get(String.format(
                OAuth2MiddlewareFactory.SESSION_SCOPE_ACCESS_TOKEN_FORMAT, this.sessionScope));

        StringBuilder token;
        if (idToken != null && this.sessionScope
                .equals(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID)) {
            LOGGER.debug("Providing id token");
            token = new StringBuilder(BEARER).append(idToken);
        } else if (accessToken != null) {
            LOGGER.debug("Providing access token for session scope : '{}'", this.sessionScope);
            token = new StringBuilder(BEARER).append(accessToken);
        } else {
            LOGGER.debug("Providing no token");
            ctx.next();
            return;
        }

        ctx.request().headers().add(HttpHeaders.AUTHORIZATION, token);
        ctx.response().headers().remove(HttpHeaders.AUTHORIZATION);

        ctx.next();
    }

}
