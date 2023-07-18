package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

public enum CSPMergeStrategy {
    EXTERNAL(DynamicConfiguration.MIDDLEWARE_CSP_MERGE_STRATEGY_EXTERNAL), INTERNAL(DynamicConfiguration.MIDDLEWARE_CSP_MERGE_STRATEGY_INTERNAL), UNION(DynamicConfiguration.MIDDLEWARE_CSP_MERGE_STRATEGY_UNION);

    private final String name;

    CSPMergeStrategy(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
