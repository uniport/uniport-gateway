package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;

import java.util.ArrayList;
import java.util.List;

/**
 The JWTOptions are part of the authentication configuration. By default, JWTOptions does not support custom claims.
 We extend the JWTOptions by adding a list of our custom claims.
 */
public class JWTClaimOptions extends JWTOptions {

    private final List<JWTClaim> claimList = new ArrayList<>();

    public JWTClaimOptions setOtherClaims(JsonArray claims) {
        if(claims != null) {
            for (Object claim : claims) {
                claimList.add(new JWTClaim((JsonObject) claim));
            }
        }
        return this;
    }

    public List<JWTClaim> getClaims() {
        return claimList;
    }
}


