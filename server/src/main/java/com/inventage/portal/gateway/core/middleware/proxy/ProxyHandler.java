package com.inventage.portal.gateway.core.middleware.proxy;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public interface ProxyHandler extends Handler<RoutingContext> {
    static ProxyHandler create(Vertx vertx, String serverHost, int serverPort) {
        return new ProxyHandlerImpl(vertx, serverHost, serverPort);
    }
}
