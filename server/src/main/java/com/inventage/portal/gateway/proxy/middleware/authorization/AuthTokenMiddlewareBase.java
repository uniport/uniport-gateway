package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Session;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AuthTokenMiddlewareBase implements Middleware {

    public static final int EXPIRATION_LEEWAY_SECONDS = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenMiddlewareBase.class);
    protected final String name;
    private final String sessionScope;

    protected AuthTokenMiddlewareBase(String name, String sessionScope) {
        this.name = name;
        this.sessionScope = sessionScope;
    }

    protected Future<String> getAuthToken(Session session) {
        try {
            final Promise<String> promise = Promise.promise();
            this.getAuthToken(session, promise);
            return promise.future();
        } catch (Throwable t) {
            LOGGER.error("error in getAuthToken", t);
            return Future.failedFuture(t);
        }
    }

    private void getAuthToken(Session session, Handler<AsyncResult<String>> handler) {
        Pair<OAuth2Auth, User> authPair = null;
        boolean idTokenDemanded = false;

        if (this.sessionScope == null) {
            final String errMsg = "No session scope found";
            LOGGER.debug("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

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
        } else if (this.sessionScope.length() != 0) {
            final String key = String.format("%s%s", this.sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
            authPair = (Pair<OAuth2Auth, User>) session.data().get(key);
        } else {
            LOGGER.debug("No token demanded");
            handler.handle(Future.succeededFuture());
            return;
        }

        if (authPair == null) {
            final String errMsg = "No user found";
            LOGGER.debug("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        final Promise<Pair<OAuth2Auth, User>> preparedUser = Promise.promise();
        final OAuth2Auth authProvider = authPair.getLeft();
        final User user = authPair.getRight();
        if (user.expired(EXPIRATION_LEEWAY_SECONDS)) {
            LOGGER.info("Refreshing access token");
            authProvider.refresh(user).onSuccess(u -> {
                final Pair<OAuth2Auth, User> refreshedAuthPair = ImmutablePair.of(authProvider, u);
                final String key = String.format("%s%s", sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
                session.put(key, refreshedAuthPair);
                preparedUser.complete(refreshedAuthPair);
            }).onFailure(err -> {
                handler.handle(Future.failedFuture(err));
            });
        } else {
            LOGGER.debug("Use existing access token");
            preparedUser.complete(authPair);
        }

        // fix: Local variable defined in an enclosing scope must be final or
        // effectively final
        final boolean finalIdTokenDemanded = idTokenDemanded;
        preparedUser.future().onSuccess(ap -> {
            final JsonObject principal = ap.getRight().principal();
            final String token = this.buildAuthToken(principal, finalIdTokenDemanded);
            handler.handle(Future.succeededFuture(token));
        }).onFailure(err -> {
            handler.handle(Future.failedFuture(err));
        });
    }

    private String buildAuthToken(JsonObject principal, boolean idTokenDemanded) {
        final String rawToken;
        if (idTokenDemanded) {
            LOGGER.debug("Providing id token");
            rawToken = principal.getString("id_token");
        } else {
            LOGGER.debug("Providing access token for session scope: '{}'", this.sessionScope);
            rawToken = principal.getString("access_token");
        }

        if (rawToken == null || rawToken.length() == 0) {
            LOGGER.warn("Token is empty");
            return "";
        }

        return rawToken;
    }
}
