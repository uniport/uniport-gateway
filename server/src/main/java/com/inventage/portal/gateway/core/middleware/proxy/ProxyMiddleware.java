package com.inventage.portal.gateway.core.middleware.proxy;

import com.inventage.portal.gateway.core.middleware.Middleware;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;

public class ProxyMiddleware implements Middleware {

    public static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    private HttpProxy httpProxy;

    public ProxyMiddleware(Vertx vertx, String serverHost, int serverPort) {
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
