package ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker;

import ch.uniport.gateway.proxy.middleware.authorization.JWTAuthVerifierMiddlewareFactoryBase;

public enum JWTClaimOperator {

    EQUALS(JWTAuthVerifierMiddlewareFactoryBase.CLAIM_OPERATOR_EQUALS),

    CONTAINS(JWTAuthVerifierMiddlewareFactoryBase.CLAIM_OPERATOR_CONTAINS),

    EQUALS_SUBSTRING_WHITESPACE(JWTAuthVerifierMiddlewareFactoryBase.CLAIM_OPERATOR_EQUALS_SUBSTRING_WHITESPACE),

    CONTAINS_SUBSTRING_WHITESPACE(JWTAuthVerifierMiddlewareFactoryBase.CLAIM_OPERATOR_CONTAINS_SUBSTRING_WHITESPACE);

    private final String name;

    JWTClaimOperator(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
