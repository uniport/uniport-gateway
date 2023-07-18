package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;

public interface CompositeCSPHandler extends CSPHandler {
    String CSP_HEADER_NAME = "Content-Security-Policy";
    String CSP_REPORT_ONLY_HEADER_NAME = "Content-Security-Policy-Report-Only";
    String REPORT_URI = "report-uri";
    String REPORT_TO = "report-to";

    static CompositeCSPHandler create() {
        return new CompositeCSPHandlerImpl();
    }

    static CompositeCSPHandler create(String mergeStrategy) {
        return new CompositeCSPHandlerImpl(mergeStrategy);
    }

    void handleResponse(RoutingContext ctx, MultiMap headers);
}