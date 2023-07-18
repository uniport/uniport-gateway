package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;

public interface CompositeCSPHandler extends CSPHandler {
    static CompositeCSPHandler create() {
        return new CompositeCSPHandlerImpl();
    }

    static CompositeCSPHandler create(String mergeStrategy) {
        return new CompositeCSPHandlerImpl(mergeStrategy);
    }

    void handleResponse(RoutingContext ctx, MultiMap headers);
}