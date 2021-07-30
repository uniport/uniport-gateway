package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

/**
 * Manages the authentication bearer. If the use is authenticated it provides either an ID token or
 * a access token as defined in the sessionScope. Access tokens are only provided if the
 * sessionScope matches the corresponding scope of the OAuth2 provider. It also ensures that no
 * token is sent to the Client.
 */
public class AuthorizationBearerMiddleware implements Middleware {

    public static final int EXPIRATION_LEEWAY_SECONDS = 5;

    public final static String BEARER = "Bearer ";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddleware.class);

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

            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, BEARER + token);

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
        Pair<OAuth2Auth, User> authPair = null;
        boolean idTokenDemanded = false;

        if (this.sessionScope.equals(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID)) {
            idTokenDemanded = true;
            // all ID tokens are the same hence take the first one
            for (String key : session.data().keySet()) {
                if (!key.endsWith(OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX)) {
                    continue;
                }
                authPair = (Pair<OAuth2Auth, User>) session.data().get(key);
                break;
            }
        } else if (this.sessionScope != null && this.sessionScope.length() != 0) {
            String key = String.format("%s%s", this.sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
            authPair = (Pair<OAuth2Auth, User>) session.data().get(key);
        } else {
            LOGGER.debug("getAuthToken: no token demanded");
            handler.handle(Future.succeededFuture());
            return;
        }

        if (authPair == null) {
            String errMsg = "No user found";
            LOGGER.debug("getAuthToken: {}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        Promise<Pair<OAuth2Auth, User>> preparedUser = Promise.promise();
        OAuth2Auth authProvider = authPair.getLeft();
        User user = authPair.getRight();
        if (user.expired(EXPIRATION_LEEWAY_SECONDS)) {
            LOGGER.info("getAuthToken: refreshing access token");
            authProvider.refresh(user).onSuccess(u -> {
                Pair<OAuth2Auth, User> refreshedAuthPair = ImmutablePair.of(authProvider, u);
                String key = String.format("%s%s", sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
                session.put(key, refreshedAuthPair);
                preparedUser.complete(refreshedAuthPair);
            }).onFailure(err -> {
                handler.handle(Future.failedFuture(err));
            });
        } else {
            LOGGER.debug("getAuthToken: use existing access token");
            preparedUser.complete(authPair);
        }

        // fix: Local variable defined in an enclosing scope must be final or effectively final
        final boolean finalIdTokenDemanded = idTokenDemanded;
        preparedUser.future().onSuccess(ap -> {
            User u = ap.getRight();
            String token = this.buildAuthToken(u.principal(), finalIdTokenDemanded);
            handler.handle(Future.succeededFuture(token));
        }).onFailure(err -> {
            handler.handle(Future.failedFuture(err));
        });
    }

    private String buildAuthToken(JsonObject principal, boolean idTokenDemanded) {
        String rawToken;
        if (idTokenDemanded) {
            LOGGER.debug("buildAuthToken: Providing id token");
            rawToken = principal.getString("id_token");
        } else {
            LOGGER.debug("buildAuthToken: Providing access token for session scope: '{}'", this.sessionScope);
            rawToken = principal.getString("access_token");
        }

        if (rawToken == null || rawToken.length() == 0) {
            LOGGER.warn("buildAuthToken: token is empty");
            return "";
        }

        return rawToken;
    }

}
