package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.ProxiedHttpServerRequest;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;

/**
 * Proxies requests and set the FORWARDED headers.
 */
public class ProxyMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddleware.class);

    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    private HttpProxy httpProxy;

    private String serverHost;

    private int serverPort;

    private UriMiddleware uriMiddleware;

    public ProxyMiddleware(Vertx vertx, String serverHost, int serverPort,
            UriMiddleware uriMiddleware) {
        this.httpProxy = HttpProxy.reverseProxy2(vertx.createHttpClient());
        this.httpProxy.target(serverPort, serverHost);
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.uriMiddleware = uriMiddleware;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!ctx.request().headers().contains(X_FORWARDED_HOST)) {
            ctx.request().headers().add(X_FORWARDED_HOST, ctx.request().host());
        }

        // URI manipulations are not allowed by Vertx-Web therefore proxied requests
        // are patched here
        HttpServerRequest request;
        if (this.uriMiddleware != null) {
            request = (new ProxiedHttpServerRequest(ctx, AllowForwardHeaders.ALL))
                    .setUriMiddleware(this.uriMiddleware);
        } else {
            request = ctx.request();
        }

        LOGGER.debug("handle: Sending request to '{}:{}{}'", this.serverHost, this.serverPort,
                request.uri());
        httpProxy.handle(request);
    }

}
