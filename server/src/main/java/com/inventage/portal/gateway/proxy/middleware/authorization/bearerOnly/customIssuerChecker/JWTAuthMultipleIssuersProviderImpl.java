package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;
import java.util.List;

/**
 */
public class JWTAuthMultipleIssuersProviderImpl extends JWTAuthProviderImpl {

    private final JWTAuthOptions config;

    private final List<String> additionalIssuers;

    /**
    */
    public JWTAuthMultipleIssuersProviderImpl(Vertx vertx, JWTAuthOptions config, JWTAuthMultipleIssuersOptions options) {
        super(vertx, config);

        this.config = config;
        additionalIssuers = options == null ? List.of() : options.getAdditionalIssuers();
    }

    @Override
    public Future<User> authenticate(Credentials credentials) {
        // If we don't have multiple issuer, we can use the built-in vertx authenticate check.
        if (additionalIssuers.isEmpty()) {
            return super.authenticate(credentials);
        }

        // Otherwise we manually check if the current jwt has one of our additional issuer included.
        // Copied from the JWTAuthProviderImpl#authenticate method.
        // https://github.com/eclipse-vertx/vertx-auth/blob/4.5.8/vertx-auth-jwt/src/main/java/io/vertx/ext/auth/jwt/impl/JWTAuthProviderImpl.java#L146-L154
        final TokenCredentials authInfo;
        try {
            // cast
            authInfo = (TokenCredentials) credentials;
            // check
            authInfo.checkValid(null);
        } catch (RuntimeException e) {
            return Future.failedFuture(e);
        }

        final JsonObject jwt = JWT.parse(authInfo.getToken());
        final JsonObject payload = jwt.getJsonObject("payload");
        final String jwtIssuer = payload.getString("iss");

        if (jwtIssuer == null) {
            return Future.failedFuture("No issuer defined in token");
        }

        boolean issuerFound = false;
        for (String possibleIssuer : additionalIssuers) {
            if (possibleIssuer.equals(jwtIssuer)) {
                issuerFound = true;
            }
        }

        // The jwt issuer is not one of the defined additional issuers, therefore we can use the built-in check.
        if (!issuerFound) {
            return super.authenticate(credentials);
        }

        // The built-in check does not support multiple possible issuer.
        // Therefore, we need to bypass the built-in issuer check by setting the issuer temporarily to null.
        // The built-in code for the check can be found at: io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl, vertx-auth-jwt:4.3.8
        final String issuer = config.getJWTOptions().getIssuer();
        config.getJWTOptions().setIssuer(null);
        final Future<User> result = super.authenticate(credentials);
        config.getJWTOptions().setIssuer(issuer);
        return result;
    }
}
