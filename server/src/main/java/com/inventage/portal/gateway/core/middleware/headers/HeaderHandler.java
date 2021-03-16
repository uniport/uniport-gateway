package com.inventage.portal.gateway.core.middleware.headers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface HeaderHandler extends Handler<RoutingContext> {
    static HeaderHandler create() {
        return new HeaderHandlerImpl();
    }
}
