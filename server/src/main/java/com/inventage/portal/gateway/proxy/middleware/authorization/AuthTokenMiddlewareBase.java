package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Session;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract middleware for middlewares operating on the Authorization Header.
 */
public abstract class AuthTokenMiddlewareBase extends TraceMiddleware {

    public static final int EXPIRATION_LEEWAY_SECONDS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenMiddlewareBase.class);

    private final Vertx vertx;
    protected final String name;
    protected final String sessionScope;

    protected AuthTokenMiddlewareBase(Vertx vertx, String name, String sessionScope) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sessionScope, "sessionScope must not be null");

        this.vertx = vertx;
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
        AuthenticationUserContext authContext = null;
        boolean idTokenDemanded = false;

        if (this.sessionScope == null) {
            final String errMsg = "No session scope found";
            LOGGER.debug("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        if (this.sessionScope.equals(OAuth2MiddlewareFactory.OAUTH2_SESSION_SCOPE_ID)) {
            idTokenDemanded = true;
            // all ID tokens are the same hence take anyone
            authContext = AuthenticationUserContext.fromSessionAtAnyScope(session).orElse(null);
        } else if (this.sessionScope.length() != 0) {
            authContext = AuthenticationUserContext.fromSessionAtScope(session, this.sessionScope).orElse(null);
        } else {
            LOGGER.debug("No token demanded");
            handler.handle(Future.succeededFuture());
            return;
        }

        if (authContext == null) {
            final String errMsg = "No user found";
            LOGGER.debug("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        final Promise<AuthenticationUserContext> preparedUser = Promise.promise();
        final OAuth2Auth authProvider = authContext.getAuthenticationProvider(vertx);
        final User user = authContext.getUser();
        if (user.expired(EXPIRATION_LEEWAY_SECONDS)) {
            LOGGER.info("Refreshing access token");
            authProvider.refresh(user).onSuccess(u -> {
                preparedUser.complete(AuthenticationUserContext.of(authProvider, u).toSessionAtScope(session, sessionScope));
            }).onFailure(err -> {
                handler.handle(Future.failedFuture(err));
            });
        } else {
            LOGGER.debug("Use existing access token");
            preparedUser.complete(authContext);
        }

        // fix: Local variable defined in an enclosing scope must be final or
        // effectively final
        final boolean finalIdTokenDemanded = idTokenDemanded;
        preparedUser.future().onSuccess(ap -> {
            final String token = this.buildAuthToken(ap, finalIdTokenDemanded);
            handler.handle(Future.succeededFuture(token));
        }).onFailure(err -> {
            handler.handle(Future.failedFuture(err));
        });
    }

    private String buildAuthToken(AuthenticationUserContext authContext, boolean idTokenDemanded) {
        final String rawToken;
        if (idTokenDemanded) {
            LOGGER.debug("Providing id token");
            rawToken = authContext.getIdToken();
        } else {
            LOGGER.debug("Providing access token for session scope: '{}'", this.sessionScope);
            rawToken = authContext.getAccessToken();
        }

        if (rawToken == null || rawToken.length() == 0) {
            LOGGER.warn("Token is empty");
            return "";
        }

        return rawToken;
    }
}
