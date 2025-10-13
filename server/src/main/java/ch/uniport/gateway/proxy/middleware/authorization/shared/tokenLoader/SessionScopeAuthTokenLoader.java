package ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader;

import ch.uniport.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import ch.uniport.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract middleware for middlewares operating on the Authorization Header.
 */
public final class SessionScopeAuthTokenLoader {

    public static final int EXPIRATION_LEEWAY_SECONDS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionScopeAuthTokenLoader.class);

    private SessionScopeAuthTokenLoader() {
    }

    public static void load(Vertx vertx, Session session, String sessionScope, Handler<AsyncResult<String>> resultHandler) {
        load(vertx, session, sessionScope)
            .onComplete(resultHandler);
    }

    public static Future<String> load(Vertx vertx, Session session, String sessionScope) {
        AuthenticationUserContext authContext = null;
        final boolean idTokenRequested = sessionScope.equals(OAuth2MiddlewareFactory.SESSION_SCOPE_ID);

        if (idTokenRequested) {
            // all ID tokens are the same hence take anyone FIXME they are not regarding the audience
            authContext = AuthenticationUserContext.fromSessionAtAnyScope(session).orElse(null);
        } else if (sessionScope.length() != 0) {
            LOGGER.debug("Loading access token for session scope: '{}'", sessionScope);
            authContext = AuthenticationUserContext.fromSessionAtScope(session, sessionScope).orElse(null);
        } else {
            LOGGER.debug("No token demanded");
            return Future.succeededFuture();
        }

        if (authContext == null) {
            final String errMsg = "No token found";
            LOGGER.debug(errMsg);
            return Future.failedFuture(errMsg);
        }

        return refreshUser(vertx, authContext, session, sessionScope)
            .map(ac -> loadAuthToken(ac, idTokenRequested));
    }

    private static Future<AuthenticationUserContext> refreshUser(Vertx vertx, AuthenticationUserContext authContext, Session session, String sessionScope) {
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

    private static String loadAuthToken(AuthenticationUserContext authContext, boolean idTokenRequested) {
        final String rawToken;
        if (idTokenRequested) {
            LOGGER.debug("Providing id token");
            rawToken = authContext.getIdToken();
        } else {
            LOGGER.debug("Providing access token");
            rawToken = authContext.getAccessToken();
        }

        if (rawToken == null || rawToken.length() == 0) {
            LOGGER.warn("Token is empty");
            return "";
        }

        return rawToken;
    }
}
