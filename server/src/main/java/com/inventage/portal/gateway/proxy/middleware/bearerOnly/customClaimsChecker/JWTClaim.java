package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonObject;

public class JWTClaim {
    final String path;
    final JWTClaimOperator operator;
    final Object value;


    JWTClaim(JsonObject claimObject) {
        this.path = claimObject.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_PATH);
        this.operator = JWTClaimOperator.valueOf(claimObject.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_OPERATOR));
        this.value = claimObject.getValue(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_VALUE);
    }
}
