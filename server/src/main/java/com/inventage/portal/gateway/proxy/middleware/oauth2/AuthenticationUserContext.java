package com.inventage.portal.gateway.proxy.middleware.oauth2;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Session;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Instance holds the received authentication tokens together with the
 * authentication provider for refreshing the tokens.
 */
public class AuthenticationUserContext {

    private final Pair<OAuth2Auth, User> pair;

    public AuthenticationUserContext(OAuth2Auth authenticationProvider, User user) {
        pair = ImmutablePair.of(authenticationProvider, user);
    }

    public static Optional<AuthenticationUserContext> fromSessionAtScope(Session session, String sessionScope) {
        final String key = String.format("%s%s", sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
        final Pair<OAuth2Auth, User> aPair = (Pair<OAuth2Auth, User>) session.data().get(key);
        return aPair != null ? Optional.of(new AuthenticationUserContext(aPair.getLeft(), aPair.getRight())) : Optional.empty();
    }

    public AuthenticationUserContext toSessionAtScope(Session session, String sessionScope) {
        session.put(String.format("%s%s", sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX), pair);
        return this;
    }

    public User getUser() {
        return pair.getRight();
    }

    public JsonObject getPrincipal() {
        return pair.getRight().principal();
    }

    public String getIdToken() {
        return getPrincipal().getString("id_token");
    }

    public String getAccessToken() {
        return getPrincipal().getString("access_token");
    }
}
