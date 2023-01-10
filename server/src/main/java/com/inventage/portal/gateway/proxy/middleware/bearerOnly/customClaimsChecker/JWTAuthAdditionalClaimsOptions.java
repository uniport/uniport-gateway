package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JWTAuthAdditionalClaimsOptions {

    private final List<JWTClaim> additionalClaims = new ArrayList<>();

    public JWTAuthAdditionalClaimsOptions setAdditionalClaims(JsonArray claims) {
        Validate.notNull(claims, "Claims can not be null");
        if (claims != null) {
            for (Object claim : claims) {
                additionalClaims.add(new JWTClaim((JsonObject) claim));
            }
        }
        return this;
    }

    public JWTAuthAdditionalClaimsOptions setAdditionalClaims(List<JWTClaim> claims) {
        Validate.notNull(claims, "Claims can not be null");
        this.additionalClaims.addAll(claims);
        return this;
    }

    public List<JWTClaim> getAdditionalClaims() {
        return new ArrayList<>(additionalClaims);
    }
}
