package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2RBAC;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;

public class MockOAuth2Auth implements OAuth2Auth {

    private JsonObject principal;
    private final int refreshedExpiresIn;

    public MockOAuth2Auth(JsonObject principal, int refreshedExpiresIn) {
        this.principal = principal;
        this.refreshedExpiresIn = refreshedExpiresIn;
    }

    @Override
    public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
        User user = this.createUser(this.principal);
        resultHandler.handle(Future.succeededFuture(user));
    }

    @Override
    public OAuth2Auth jWKSet(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture());
        return this;
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
    public OAuth2Auth refresh(User user, Handler<AsyncResult<User>> handler) {
        this.principal.put("expires_in", refreshedExpiresIn);
        User newUser = this.createUser(this.principal);
        handler.handle(Future.succeededFuture(newUser));

        return this;
    }

    @Override
    public OAuth2Auth revoke(User user, String tokenType, Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture());
        return this;
    }

    @Override
    public OAuth2Auth userInfo(User user, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject userInfo = new JsonObject();
        handler.handle(Future.succeededFuture(userInfo));
        return this;
    }

    @Override
    public String endSessionURL(User user, JsonObject params) {
        return "";
    }

    @Override
    public OAuth2Auth decodeToken(String token, Handler<AsyncResult<AccessToken>> handler) {
        AccessToken accessToken = new AccessTokenImpl();
        handler.handle(Future.succeededFuture(accessToken));
        return this;
    }

    @Override
    public OAuth2Auth introspectToken(String token, String tokenType, Handler<AsyncResult<AccessToken>> handler) {
        AccessToken accessToken = new AccessTokenImpl();
        handler.handle(Future.succeededFuture(accessToken));
        return this;
    }

    @Override
    public OAuth2FlowType getFlowType() {
        return OAuth2FlowType.CLIENT;
    }

    @Override
    public OAuth2Auth rbacHandler(OAuth2RBAC rbac) {
        return this;
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
}
