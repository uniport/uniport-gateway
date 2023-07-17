package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

public enum CSPMergeStrategy {
    EXTERNAL(DynamicConfiguration.MIDDLEWARE_CSP_EXTERNAL_MERGE_POLICY_EXTERNAL), INTERNAL(DynamicConfiguration.MIDDLEWARE_CSP_EXTERNAL_MERGE_POLICY_INTERNAL), UNION(DynamicConfiguration.MIDDLEWARE_CSP_EXTERNAL_MERGE_POLICY_UNION);

    private final String name;

    CSPMergeStrategy(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
