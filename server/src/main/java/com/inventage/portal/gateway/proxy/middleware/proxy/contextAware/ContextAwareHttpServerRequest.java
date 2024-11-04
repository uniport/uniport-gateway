package com.inventage.portal.gateway.proxy.middleware.proxy.contextAware;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.http.impl.HttpServerRequestWrapper;
import io.vertx.ext.web.RoutingContext;

/**
 */
public class ContextAwareHttpServerRequest extends HttpServerRequestWrapper {

    private RoutingContext ctx;

    public ContextAwareHttpServerRequest(HttpServerRequest request, RoutingContext ctx) {
        super((HttpServerRequestInternal) request);
        this.ctx = ctx;
    }

    /**
     */
    public RoutingContext routingContext() {
        return ctx;
    }
}
