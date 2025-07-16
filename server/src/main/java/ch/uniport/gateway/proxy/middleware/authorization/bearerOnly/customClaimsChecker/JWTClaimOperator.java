package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker;

import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;

public enum JWTClaimOperator {

    EQUALS(WithAuthHandlerMiddlewareFactoryBase.CLAIM_OPERATOR_EQUALS),

    CONTAINS(WithAuthHandlerMiddlewareFactoryBase.CLAIM_OPERATOR_CONTAINS),

    EQUALS_SUBSTRING_WHITESPACE(WithAuthHandlerMiddlewareFactoryBase.CLAIM_OPERATOR_EQUALS_SUBSTRING_WHITESPACE),

    CONTAINS_SUBSTRING_WHITESPACE(WithAuthHandlerMiddlewareFactoryBase.CLAIM_OPERATOR_CONTAINS_SUBSTRING_WHITESPACE);

    private final String name;

    JWTClaimOperator(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
