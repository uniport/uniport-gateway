package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;

/**
 * 
 * The CompositeCSPHandler is required to work a little bit different than the CSPHandler by vertx-web.
 * 
 * First it needs to be able to union the CSP configuration of multiple middlewares on incoming request
 * (The routing context should be used for trasmitting information between middlewares).
 * 
 * Additionally, it is needs to do the same for middlewares on the incoming response.
 * Hence, the handleResponse method.
 * 
 * One special case occurs for the first middleware on the incoming response:
 * The backend might send its own CSP, therefore the middleware needs to either use
 * - the internally aggregated CSP
 * - the externally supplied CSP
 * - or union both internal and external CSP.
 * 
 * (incoming request) ---> csp1 --> csp2 --> ... --> cspN --> backend
 * (outgoing response) <-- csp1 <-- csp2 <-- ... <-- cspN <--
 * 
 */
public interface CompositeCSPHandler extends CSPHandler {
    CSPMergeStrategy DEFAULT_CSP_MERGE_STRATEGY = CSPMergeStrategy.UNION;

    String CSP_HEADER_NAME = "Content-Security-Policy";
    String CSP_REPORT_ONLY_HEADER_NAME = "Content-Security-Policy-Report-Only";
    String REPORT_URI = "report-uri";
    String REPORT_TO = "report-to";

    /**
    */
    static CompositeCSPHandler create() {
        return new CompositeCSPHandlerImpl(DEFAULT_CSP_MERGE_STRATEGY);
    }

    /**
    */
    static CompositeCSPHandler create(CSPMergeStrategy mergeStrategy) {
        return new CompositeCSPHandlerImpl(mergeStrategy);
    }

    /**
    */
    void handleResponse(RoutingContext ctx, MultiMap headers);
}