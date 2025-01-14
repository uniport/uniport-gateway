package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker;

import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;

public enum JWTClaimOperator {

    EQUALS(WithAuthHandlerMiddlewareFactoryBase.MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_EQUALS),

    CONTAINS(WithAuthHandlerMiddlewareFactoryBase.MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_CONTAINS),

    EQUALS_SUBSTRING_WHITESPACE(WithAuthHandlerMiddlewareFactoryBase.MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_EQUALS_SUBSTRING_WHITESPACE),

    CONTAINS_SUBSTRING_WHITESPACE(WithAuthHandlerMiddlewareFactoryBase.MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_CONTAINS_SUBSTRING_WHITESPACE);

    private final String name;

    JWTClaimOperator(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
