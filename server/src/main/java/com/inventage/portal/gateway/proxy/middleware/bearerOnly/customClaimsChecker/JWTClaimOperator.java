package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

public enum JWTClaimOperator {
    EQUALS(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_OPERATOR_EQUALS),
    CONTAINS(DynamicConfiguration.MIDDLEWARE_BEARER_ONLY_CLAIM_OPERATOR_CONTAINS);

    private final String name;
    JWTClaimOperator(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }


}
