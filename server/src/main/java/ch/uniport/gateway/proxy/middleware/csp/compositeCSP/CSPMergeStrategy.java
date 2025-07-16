package ch.uniport.gateway.proxy.middleware.csp.compositeCSP;

import ch.uniport.gateway.proxy.middleware.csp.CSPMiddlewareFactory;

public enum CSPMergeStrategy {

    EXTERNAL(CSPMiddlewareFactory.MERGE_STRATEGY_EXTERNAL),

    INTERNAL(CSPMiddlewareFactory.MERGE_STRATEGY_INTERNAL),

    UNION(CSPMiddlewareFactory.MERGE_STRATEGY_UNION);

    private final String name;

    CSPMergeStrategy(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
