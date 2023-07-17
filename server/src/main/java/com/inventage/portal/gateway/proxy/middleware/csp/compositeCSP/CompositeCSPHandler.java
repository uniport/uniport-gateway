package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import io.vertx.ext.web.handler.CSPHandler;

public interface CompositeCSPHandler extends CSPHandler {
    static CompositeCSPHandlerImpl create() {
        return new CompositeCSPHandlerImpl();
    }

    static CompositeCSPHandlerImpl create(String mergeStrategy) {
        return new CompositeCSPHandlerImpl(mergeStrategy);
    }
}