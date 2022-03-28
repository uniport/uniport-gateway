package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonObject;

/**
 JWTClaim Model.
 */
public class JWTClaim {

    final String path;
    final JWTClaimOperator operator;
    final Object value;


    /**
     *
     * @param claimObject json Object containing the entries of the claim.
     *                    path: In JsonPath syntax (https://github.com/json-path/JsonPath), which describes the path to the entry in the payload to be checked.
     *                    operator: The operator that defines the rule for the check.
     *                    value: The claim value, that is compared to the payload entry
     */
    JWTClaim(JsonObject claimObject) {
        this.path = claimObject.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_PATH);
        this.operator = JWTClaimOperator.valueOf(claimObject.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_OPERATOR));
        this.value = claimObject.getValue(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_VALUE);
    }
}
