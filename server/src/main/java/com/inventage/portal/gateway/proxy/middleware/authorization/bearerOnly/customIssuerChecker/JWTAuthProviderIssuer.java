package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.util.List;

public interface JWTAuthProviderIssuer extends AuthenticationProvider {
    static JWTAuth create(Vertx vertx, JWTAuthOptions config) {
        return new JWTAuthProviderIssuerImpl(vertx, config);
    }

    static JWTAuth create(Vertx vertx, JWTAuthOptions config, List<String> additionalIssuers) {
        return new JWTAuthProviderIssuerImpl(vertx, config, additionalIssuers);
    }

    static JWTAuth create(Vertx vertx, JWTAuthOptions config, JsonArray additionalIssuers) {
        if (additionalIssuers == null) {
            return JWTAuth.create(vertx, config);
        }
        return new JWTAuthProviderIssuerImpl(vertx, config, additionalIssuers.getList());
    }

}
