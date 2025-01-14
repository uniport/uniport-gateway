package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import com.inventage.portal.gateway.proxy.middleware.csp.CSPMiddlewareFactory;

public enum CSPMergeStrategy {

    EXTERNAL(CSPMiddlewareFactory.MIDDLEWARE_CSP_MERGE_STRATEGY_EXTERNAL),

    INTERNAL(CSPMiddlewareFactory.MIDDLEWARE_CSP_MERGE_STRATEGY_INTERNAL),

    UNION(CSPMiddlewareFactory.MIDDLEWARE_CSP_MERGE_STRATEGY_UNION);

    private final String name;

    CSPMergeStrategy(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
