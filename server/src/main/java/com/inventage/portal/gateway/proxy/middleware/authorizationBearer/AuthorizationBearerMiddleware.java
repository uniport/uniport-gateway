package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AuthorizationBearerMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

    private final static String BEARER = "Bearer ";

    private final static String ID_TOKEN = "id_token";
    private final static String ACCESS_TOKEN = "access_token";
    private final static String EXPIRES_IN = "expires_in";

    private final static String REFRESH_TOKEN = "refresh_token";
    private final static String REFRESH_EXPIRES_IN = "refresh_expires_in";

    private String sessionScope;

    public AuthorizationBearerMiddleware(String sessionScope) {
        this.sessionScope = sessionScope;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.user() != null && ctx.user().principal() != null) {

            // TODO read tokens from session map
            JsonObject principal = ctx.user().principal();

            int expiresIn = principal.getInteger(EXPIRES_IN);
            if (expiresIn <= 0) {
                LOGGER.warn("Ignoring expired token");
                ctx.next();
                return;
            }

            StringBuilder token;
            if (this.sessionScope.equals(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID)) {
                LOGGER.debug("Providing id token");
                String idToken = principal.getString(ID_TOKEN);
                token = new StringBuilder(BEARER).append(idToken);
            } else {
                LOGGER.debug("Providing access token for session scope : '{}'", this.sessionScope);
                String accessToken = principal.getString(ACCESS_TOKEN);
                token = new StringBuilder(BEARER).append(accessToken);
            }

            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, token);
            ctx.response().headers().remove(HttpHeaders.AUTHORIZATION);
        }

        ctx.next();
    }

}
