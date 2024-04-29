package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 */
public class JWTAuthAdditionalClaimsOptions {

    private final List<JWTClaim> additionalClaims = new ArrayList<>();

    /**
    */
    public List<JWTClaim> getAdditionalClaims() {
        return new ArrayList<>(additionalClaims);
    }

    /**
    */
    public JWTAuthAdditionalClaimsOptions setAdditionalClaims(JsonArray claims) {
        Validate.notNull(claims, "Claims can not be null");
        if (claims != null) {
            for (Object claim : claims) {
                additionalClaims.add(new JWTClaim((JsonObject) claim));
            }
        }
        return this;
    }

    /**
    */
    public JWTAuthAdditionalClaimsOptions setAdditionalClaims(List<JWTClaim> claims) {
        Validate.notNull(claims, "Claims can not be null");
        this.additionalClaims.addAll(claims);
        return this;
    }
}
