package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;

import java.util.ArrayList;
import java.util.List;


public class JWTClaimOptions extends JWTOptions {

    private List<JWTClaim> claimList = new ArrayList<>();

    public JWTClaimOptions setOtherClaims(JsonArray claims){

        for (int i = 0; i < claims.size(); i++){
            claimList.add(new JWTClaim(claims.getJsonObject(i)));
        }
        return this;
    }

    public List<JWTClaim> getClaims() {
        return claimList;
    }
}


