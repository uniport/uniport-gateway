package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import io.vertx.core.Future;
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
        AuthenticationUserContext authContext = null;
        final boolean idTokenDemanded = sessionScope.equals(OAuth2MiddlewareFactory.SESSION_SCOPE_ID);

        if (idTokenDemanded) {
            // all ID tokens are the same hence take anyone
            authContext = AuthenticationUserContext.fromSessionAtAnyScope(session).orElse(null);
        } else if (sessionScope.length() != 0) {
            authContext = AuthenticationUserContext.fromSessionAtScope(session, sessionScope).orElse(null);
        } else {
            LOGGER.debug("No token demanded");
            return Future.succeededFuture();
        }

        if (authContext == null) {
            final String errMsg = "No user found";
            LOGGER.debug("{}", errMsg);
            return Future.failedFuture(errMsg);
        }

        return refreshUser(authContext, session)
            .map(ac -> buildAuthToken(ac, idTokenDemanded));
    }

    private Future<AuthenticationUserContext> refreshUser(AuthenticationUserContext authContext, Session session) {
        final User user = authContext.getUser();
        if (!user.expired(EXPIRATION_LEEWAY_SECONDS)) {
            LOGGER.debug("Use existing access token");
            return Future.succeededFuture(authContext);
        }

        LOGGER.info("Refreshing access token");
        final OAuth2Auth authProvider = authContext.getAuthenticationProvider(vertx);
        return authProvider.refresh(user)
            .map(u -> AuthenticationUserContext.of(authProvider, u).toSessionAtScope(session, sessionScope));
    }

    private String buildAuthToken(AuthenticationUserContext authContext, boolean idTokenDemanded) {
        final String rawToken;
        if (idTokenDemanded) {
            LOGGER.debug("Providing id token");
            rawToken = authContext.getIdToken();
        } else {
            LOGGER.debug("Providing access token for session scope: '{}'", sessionScope);
            rawToken = authContext.getAccessToken();
        }

        if (rawToken == null || rawToken.length() == 0) {
            LOGGER.warn("Token is empty");
            return "";
        }

        return rawToken;
    }
}
