package com.inventage.portal.gateway.core.middleware.proxy;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;

public class ProxyHandlerImpl implements ProxyHandler {

    public static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    private HttpProxy httpProxy;

    public ProxyHandlerImpl(Vertx vertx, String serverHost, int serverPort) {
        this.httpProxy = HttpProxy.reverseProxy2(vertx.createHttpClient());
        this.httpProxy.target(serverPort, serverHost);
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!ctx.request().headers().contains(X_FORWARDED_HOST)) {
            ctx.request().headers().add(X_FORWARDED_HOST, ctx.request().host());
        }

        httpProxy.handle(ctx.request());
    }
}
