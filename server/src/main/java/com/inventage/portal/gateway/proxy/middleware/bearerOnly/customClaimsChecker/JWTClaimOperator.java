package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

public enum JWTClaimOperator {
    EQUALS("equals"), CONTAINS("contains");

    private final String name;
    private JWTClaimOperator(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
