package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * The JWTOptions are part of the authentication configuration. By default, JWTOptions does not support custom claims.
 * We extend the JWTOptions by adding a list of our custom claims.
 * https://github.com/vert-x3/vertx-auth/blob/4.0.3/vertx-auth-common/src/main/java/io/vertx/ext/auth/JWTOptions.java
 */
public class JWTClaimOptions extends JWTOptions {

    private final List<JWTClaim> additionalClaims = new ArrayList<>();

    public JWTClaimOptions setAdditionalClaims(JsonArray claims) {
        Validate.notNull(claims, "Claims can not be null");
        if (claims != null) {
            for (Object claim : claims) {
                additionalClaims.add(new JWTClaim((JsonObject) claim));
            }
        }
        return this;
    }

    public JWTClaimOptions setAdditionalClaims(List<JWTClaim> claims) {
        Validate.notNull(claims, "Claims can not be null");
        this.additionalClaims.addAll(claims);
        return this;
    }

    public List<JWTClaim> getClaims() {
        return additionalClaims;
    }
}
