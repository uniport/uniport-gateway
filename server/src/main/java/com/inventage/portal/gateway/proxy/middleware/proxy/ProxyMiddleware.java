package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.ProxiedHttpServerRequest;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.tracing.TracingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;


/**
 * Proxies requests and set the FORWARDED headers.
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling
 */
public class ProxyMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddleware.class);

    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    private HttpProxy httpProxy;

    private String serverHost;
    private int serverPort;

    public ProxyMiddleware(Vertx vertx, String serverHost, int serverPort) {
        this.httpProxy = HttpProxy.reverseProxy2(vertx.createHttpClient(
                new HttpClientOptions()
                        .setTracingPolicy(TracingPolicy.PROPAGATE)));
        this.httpProxy.target(serverPort, serverHost);
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    public void handle(RoutingContext ctx) {
        useOrSetHeader(X_FORWARDED_HOST, ctx.request().host(), ctx.request().headers());
        useOrSetHeader(X_FORWARDED_PROTO, ctx.request().scheme(), ctx.request().headers());

        // Some manipulations are
        // * not allowed by Vertx-Web
        // * or have to be made on the response of the forwarded request
        HttpServerRequest request = new ProxiedHttpServerRequest(ctx, AllowForwardHeaders.ALL);

        LOGGER.debug("handle: Sending request to '{}:{}{}'", this.serverHost, this.serverPort, request.uri());
        httpProxy.handle(request);
    }

    /**
     * If the given header name is already contained in the request, this header will be used, otherwise the given header value is used.
     *
     * @param headerName to check the request for
     * @param headerValue to use if the header name is not yet in the request
     * @param headers of the request
     */
    protected void useOrSetHeader(String headerName, String headerValue, MultiMap headers) {
        if (headers.contains(headerName)) { // use
            LOGGER.debug("handle: using provided header '{}' with '{}'", headerName, headers.get(headerName));
        }
        else { // set
            headers.add(headerName, headerValue);
            LOGGER.debug("handle: set header '{}' to '{}'", headerName, headers.get(headerName));
        }
    }
}
