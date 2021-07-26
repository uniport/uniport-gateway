package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import java.util.Optional;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

/**
 * Manages the authentication bearer. If the use is authenticated it provides either an ID token or
 * a access token as defined in the sessionScope. Access tokens are only provided if the
 * sessionScope matches the corresponding scope of the OAuth2 provider. It also ensures that no
 * token is sent to the Client.
 */
public class AuthorizationBearerMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddleware.class);

    private final static String BEARER = "Bearer ";

    private final int EXPIRATION_LEEWAY_SECONDS = 5;

    private String sessionScope;

    public AuthorizationBearerMiddleware(String sessionScope) {
        this.sessionScope = sessionScope;
    }

    @Override
    public void handle(RoutingContext ctx) {
        this.getAuthToken(ctx.session()).onSuccess(token -> {
            if (token == null || token.length() == 0) {
                LOGGER.debug("handle: Skipping empty token");
                ctx.next();
                return;
            }

            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, token);

            Handler<MultiMap> respHeadersModifier = headers -> {
                headers.remove(HttpHeaders.AUTHORIZATION);
            };
            this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);

            ctx.next();
        }).onFailure(err -> {
            LOGGER.debug("handle: Providing no token '{}'", err.getMessage());
            ctx.next();
        });
    }

    private Future<String> getAuthToken(Session session) {
        Promise<String> promise = Promise.promise();
        this.getAuthToken(session, promise);
        return promise.future();
    }

    private void getAuthToken(Session session, Handler<AsyncResult<String>> handler) {
        AccessToken accessToken = null;
        boolean idTokenDemanded = false;

        if (this.sessionScope.equals(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID)) {
            idTokenDemanded = true;
            // all ID tokens are the same hence take the first one
            for (String key : session.data().keySet()) {
                if (!key.endsWith("_access_token")) {
                    continue;
                }
                accessToken = (AccessToken) session.data().get(key);
                break;
            }
        } else if (this.sessionScope != null && this.sessionScope.length() != 0) {
            accessToken = (AccessToken) session.data()
                    .get(String.format(OAuth2MiddlewareFactory.SESSION_SCOPE_ACCESS_TOKEN_FORMAT, this.sessionScope));
        } else {
            LOGGER.debug("getAuthToken: no token demanded");
            handler.handle(Future.succeededFuture());
            return;
        }

        if (accessToken == null) {
            String errMsg = "No token found";
            LOGGER.debug("getAuthToken: {}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        // fix: Local variable defined in an enclosing scope must be final or effectively final
        final AccessToken finalAccessToken = accessToken;
        final boolean finalIdTokenDemanded = idTokenDemanded;

        if (accessToken.expired(EXPIRATION_LEEWAY_SECONDS)) {
            accessToken.refresh().onSuccess(ar -> {
                String token = this.buildAuthToken(finalAccessToken, finalIdTokenDemanded);
                handler.handle(Future.succeededFuture(token));
            }).onFailure(err -> {
                handler.handle(Future.failedFuture(err));
            });
        } else {
            String token = this.buildAuthToken(accessToken, idTokenDemanded);
            handler.handle(Future.succeededFuture(token));
        }
    }

    private String buildAuthToken(AccessToken accessToken, boolean idTokenDemanded) {
        String rawToken;
        if (idTokenDemanded) {
            LOGGER.debug("buildAuthToken: Providing id token");
            rawToken = accessToken.opaqueIdToken();
        } else {
            LOGGER.debug("buildAuthToken: Providing access token for session scope: '{}'", this.sessionScope);
            rawToken = accessToken.opaqueAccessToken();
        }

        if (rawToken == null || rawToken.length() == 0) {
            LOGGER.warn("buildAuthToken: token is empty");
            return "";
        }

        return new StringBuilder(BEARER).append(rawToken).toString();
    }

}
