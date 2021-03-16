package com.inventage.portal.gateway.core.middleware.proxy;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;

public class ProxyHandlerImpl implements ProxyHandler {

    private HttpProxy httpProxy;

    public ProxyHandlerImpl(Vertx vertx, String serverHost, int serverPort) {
        this.httpProxy = HttpProxy.reverseProxy2(vertx.createHttpClient());
        this.httpProxy.target(serverPort, serverHost);
    }

    @Override
    public void handle(RoutingContext ctx) {
        httpProxy.handle(ctx.request());
    }
}
