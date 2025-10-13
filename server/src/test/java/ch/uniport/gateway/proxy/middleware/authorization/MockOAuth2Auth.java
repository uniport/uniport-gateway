package ch.uniport.gateway.proxy.middleware.authorization;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;

public class MockOAuth2Auth implements OAuth2Auth {

    private final String host;
    private final int port;
    private final int refreshedExpiresIn;
    private final JsonObject principal;

    public MockOAuth2Auth(JsonObject principal) {
        this("localhost", 1234, principal, 0);
    }

    public MockOAuth2Auth(JsonObject principal, int refreshedExpiresIn) {
        this("localhost", 1234, principal, refreshedExpiresIn);
    }

    public MockOAuth2Auth(String host, int port, JsonObject principal, int refreshedExpiresIn) {
        this.host = host;
        this.port = port;
        this.principal = principal;
        this.refreshedExpiresIn = refreshedExpiresIn;
    }

    public static User createUser(JsonObject json) {
        // update the principal
        final User user = User.create(json);
        final long now = System.currentTimeMillis() / 1000;

        // compute the expires_at if any
        if (json.containsKey("expires_in")) {
            Long expiresIn;
            try {
                expiresIn = json.getLong("expires_in");
            } catch (ClassCastException e) {
                // for some reason someone decided to send a number as a String...
                expiresIn = Long.valueOf(json.getString("expires_in"));
            }
            // don't interfere with the principal object
            user.attributes().put("iat", now).put("exp", now + expiresIn);
        }

        if (json.containsKey("access_token")) {
            final String token = json.getString("access_token");
            user.attributes().put("accessToken", token);
        }

        if (json.containsKey("id_token")) {
            final String token = json.getString("id_token");
            user.attributes().put("idToken", token);
        }

        return user;
    }

    @Override
    public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
        final User user = createUser(this.principal);
        resultHandler.handle(Future.succeededFuture(user));
    }

    @Override
    public Future<Void> jWKSet() {
        return Future.succeededFuture();
    }

    @Override
    public OAuth2Auth missingKeyHandler(Handler<String> handler) {
        return this;
    }

    @Override
    public String authorizeURL(JsonObject params) {
        return "";
    }

    @Override
    public String authorizeURL(OAuth2AuthorizationURL url) {
        return "";
    }

    @Override
    public Future<User> refresh(User user) {
        this.principal.put("expires_in", refreshedExpiresIn);
        final User newUser = createUser(this.principal);
        return Future.succeededFuture(newUser);
    }

    @Override
    public Future<Void> revoke(User user, String tokenType) {
        return Future.succeededFuture();
    }

    @Override
    public Future<JsonObject> userInfo(User user) {
        final JsonObject userInfo = new JsonObject();
        return Future.succeededFuture(userInfo);
    }

    @Override
    public String endSessionURL(User user, JsonObject params) {
        return endSessionURL(user);
    }

    @Override
    public String endSessionURL(User user) {
        return "http://" + host + ":" + port + "/";
    }

    @Override
    public void close() {
    }
}
