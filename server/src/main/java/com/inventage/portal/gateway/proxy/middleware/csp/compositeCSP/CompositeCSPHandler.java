package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import io.vertx.ext.web.handler.CSPHandler;

public interface CompositeCSPHandler extends CSPHandler {
    static CSPHandler create() {
        return new CompositeCSPHandlerImpl();
    }
}