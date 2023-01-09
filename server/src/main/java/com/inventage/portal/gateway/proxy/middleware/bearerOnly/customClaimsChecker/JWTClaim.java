package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import org.apache.commons.lang3.Validate;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

import io.vertx.core.json.JsonObject;

/**
 * JWTClaim Model.
 */
public class JWTClaim {

    final String path;
    final JWTClaimOperator operator;
    final Object value;

    /**
     * @param claimObject json Object containing the entries of the claim.
     *                    path: In JsonPath syntax (https://github.com/json-path/JsonPath), which describes the path to the entry in the payload to be checked.
     *                    operator: The operator that defines the rule for the check.
     *                    value: The claim value, that is compared to the payload entry
     */
    public JWTClaim(JsonObject claimObject) {
        this.path = claimObject.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_PATH);
        this.operator = JWTClaimOperator
                .valueOf(claimObject.getString(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_OPERATOR));
        this.value = claimObject.getValue(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_VALUE);
        validateCheck();
    }

    public JWTClaim(String path, JWTClaimOperator operator, Object value) {
        this.path = path;
        this.operator = operator;
        this.value = value;
        validateCheck();
    }

    private void validateCheck() {
        Validate.notNull(path, "Path can not be null");
        Validate.notNull(operator, "Operator can not be null");
        Validate.notNull(value, "Value can not be null");
    }

}
