package ch.uniport.gateway.proxy.middleware.passAuthorization;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;

public class MockJWTAuth implements JWTAuth {

    private final JsonObject principal;
    private final String correctToken;

    public MockJWTAuth(JsonObject principal, String correctToken) {
        this.principal = principal;
        this.correctToken = correctToken;
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
        String token = getTokenFromCredentials(credentials);
        if (token != null && token.equals(correctToken)) {
            User user = createUser(this.principal);
            resultHandler.handle(Future.succeededFuture(user));
        } else {
            resultHandler.handle(Future.failedFuture("not authorized"));
        }
    }

    private String getTokenFromCredentials(JsonObject credentials) {
        if (credentials.containsKey("token")) {
            return credentials.getString("token");
        } else {
            return null;
        }
    }

    @Override
    public String generateToken(JsonObject claims, JWTOptions options) {
        return "";
    }

    @Override
    public String generateToken(JsonObject claims) {
        return "";
    }
}
