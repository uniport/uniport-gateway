package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;

public class MockAccessTokenImpl extends AccessTokenImpl {

    private final int refreshedExpiresIn;

    public MockAccessTokenImpl(JsonObject principal, int refreshedExpiresIn) {
        super(principal, null);
        this.refreshedExpiresIn = refreshedExpiresIn;
    }

    @Override
    public AccessToken refresh(Handler<AsyncResult<Void>> callback) {
        User user = User.create(this.principal().copy().put("expires_in", refreshedExpiresIn));

        // merge properties
        attributes().mergeIn(user.attributes());
        principal().mergeIn(user.principal());
        callback.handle(Future.succeededFuture());

        return this;
    }

    @Override
    public AccessToken revoke(String token_type, Handler<AsyncResult<Void>> callback) {
        principal().remove(token_type);
        callback.handle(Future.succeededFuture());
        return this;
    }

    @Override
    public AccessToken userInfo(Handler<AsyncResult<JsonObject>> callback) {
        callback.handle(Future.succeededFuture(new JsonObject()));
        return this;
    }

}
