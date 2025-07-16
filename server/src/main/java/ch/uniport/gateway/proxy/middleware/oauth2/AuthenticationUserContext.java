package ch.uniport.gateway.proxy.middleware.oauth2;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.ClusterSerializable;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.impl.UserConverter;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Session;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance holds the received authentication tokens together with the
 * authentication provider for refreshing the tokens.
 */
public class AuthenticationUserContext implements ClusterSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationUserContext.class);

    private static final String SESSION_SCOPE_SUFFIX = "_session";

    private static final String FIELD_AUTHENTICATION_PROVIDER = "authenticationProvider";
    private static final String FIELD_USER = "user";
    private static final String FIELD_SESSION_SCOPE = "sessionScope";

    private OAuth2ApiWrapper authenticationProvider;
    private User user;
    private String sessionScope;

    public AuthenticationUserContext() {
        // do not remove: constructor required by AbstractSession#readDataFromBuffer for
        // ClusterSerializable
    }

    private AuthenticationUserContext(OAuth2Auth authenticationProvider, User user, String sessionScope) {
        Objects.requireNonNull(authenticationProvider);
        Objects.requireNonNull(user);
        Objects.requireNonNull(sessionScope);

        this.authenticationProvider = new OAuth2ApiWrapper(authenticationProvider);
        this.user = user;
        this.sessionScope = sessionScope;
    }

    public static AuthenticationUserContext of(OAuth2Auth authenticationProvider, User user) {
        Objects.requireNonNull(authenticationProvider);
        Objects.requireNonNull(user);

        if (!(user instanceof UserImpl)) {
            throw new IllegalArgumentException(String.format("illegal user type: %s", user.toString()));
        }

        return new AuthenticationUserContext(authenticationProvider, user, "");
    }

    public static List<AuthenticationUserContext> all(Session session) {
        final List<AuthenticationUserContext> authContexts = new LinkedList<>();
        for (String key : session.data().keySet()) {
            LOGGER.debug("Processing {}: {}", key, session.data().get(key));
            if (!key.endsWith(SESSION_SCOPE_SUFFIX)) {
                continue;
            }
            authContexts.add((AuthenticationUserContext) session.data().get(key));
        }
        return authContexts;
    }

    public static Optional<AuthenticationUserContext> fromSessionAtAnyScope(Session session) {
        for (String key : session.data().keySet()) {
            if (!key.endsWith(SESSION_SCOPE_SUFFIX)) {
                continue;
            }
            return Optional.of((AuthenticationUserContext) session.data().get(key));
        }
        return Optional.empty();
    }

    public static Optional<AuthenticationUserContext> fromSessionAtScope(Session session, String sessionScope) {
        final String key = String.format("%s%s", sessionScope, SESSION_SCOPE_SUFFIX);
        final AuthenticationUserContext authContext = (AuthenticationUserContext) session.data().get(key);
        return authContext != null ? Optional.of(authContext) : Optional.empty();
    }

    public static void deleteAll(Session session) {
        session.data().keySet().stream()
            .filter(key -> key.endsWith(SESSION_SCOPE_SUFFIX))
            .map(key -> session.remove(key))
            .collect(Collectors.toList());
    }

    public AuthenticationUserContext toSessionAtScope(Session session, String sessionScope) {
        this.sessionScope = sessionScope;
        session.put(String.format("%s%s", sessionScope, SESSION_SCOPE_SUFFIX), this);
        return this;
    }

    public OAuth2Auth getAuthenticationProvider(Vertx vertx) {
        return this.authenticationProvider.getDelegate(vertx);
    }

    public User getUser() {
        return this.user;
    }

    public JsonObject getPrincipal() {
        return getUser().principal();
    }

    public String getIdToken() {
        return getPrincipal().getString("id_token");
    }

    public String getAccessToken() {
        return getPrincipal().getString("access_token");
    }

    public String getSessionScope() {
        return this.sessionScope;
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        final JsonObject json = new JsonObject();
        json.put(FIELD_AUTHENTICATION_PROVIDER, authenticationProvider.toJson());
        json.put(FIELD_USER, UserConverter.encode(user));
        json.put(FIELD_SESSION_SCOPE, this.sessionScope);

        json.writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        final JsonObject json = new JsonObject();
        final int read = json.readFromBuffer(pos, buffer);

        authenticationProvider = new OAuth2ApiWrapper(json.getJsonObject(FIELD_AUTHENTICATION_PROVIDER));
        user = UserConverter.decode(json.getJsonObject(FIELD_USER));
        sessionScope = json.getString(FIELD_SESSION_SCOPE);

        return read;
    }
}
