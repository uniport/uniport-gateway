package com.inventage.portal.gateway.proxy.middleware.proxy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyRequest;
import io.vertx.httpproxy.ProxyResponse;

/**
 * Proxies requests and set the FORWARDED headers.
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling
 */
public class ProxyMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddleware.class);

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

    private HttpProxy httpProxy;

    private String serverHost;
    private int serverPort;

    public ProxyMiddleware(Vertx vertx, String serverHost, int serverPort) {
        this.httpProxy = HttpProxy.reverseProxy(vertx.createHttpClient());
        this.httpProxy.origin(serverPort, serverHost);
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        addIncomingRequestInterceptor(this.httpProxy);
        addOutgoingResponseInterceptor(this.httpProxy);
    }

    @Override
    public void handle(RoutingContext ctx) {
        useOrSetHeader(X_FORWARDED_PROTO, ctx.request().scheme(), ctx.request().headers());
        useOrSetHeader(X_FORWARDED_HOST, ctx.request().host(), ctx.request().headers());
        useOrSetHeader(X_FORWARDED_PORT, String.valueOf(
                portFromHostValue(ctx.request().headers().get(X_FORWARDED_HOST),
                        portFromHostValue(ctx.request().host(), -1))),
                ctx.request().headers());

        LOGGER.debug("Sending to '{}:{}{}'", this.serverHost, this.serverPort, ctx.request().uri());
        httpProxy.handle(ctx.request());
    }

    protected void addIncomingRequestInterceptor(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext ctx) {
                ProxyRequest incomingRequest = ctx.request();

                // TODO add request modifiers

                // Does not work because the RoutingContext is different from the ProxyContext
                // final StringBuilder uri = new StringBuilder(incomingRequest.getURI());
                // final List<Handler<StringBuilder>> modifiers = ctx.get(Middleware.REQUEST_URI_MODIFIERS, List.class);
                // if (modifiers != null) {
                //     if (modifiers.size() > 1) {
                //         LOGGER.info("Multiple URI modifiers declared: %s (total %d)", modifiers, modifiers.size());
                //     }
                //     for (Handler<StringBuilder> modifier : modifiers) {
                //         modifier.handle(uri);
                //     }
                // }
                // incomingRequest.setURI(uri.toString());

                // Continue the interception chain
                return ctx.sendRequest();
            }
        });
    }

    protected void addOutgoingResponseInterceptor(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<Void> handleProxyResponse(ProxyContext ctx) {
                ProxyResponse outgoingResponse = ctx.response();

                // TODO add response modifers

                // Does not work because the RoutingContext is different from the ProxyContext
                // final List<Handler<MultiMap>> modifiers = ctx.get(Middleware.RESPONSE_HEADERS_MODIFIERS, List.class);
                // if (modifiers != null) {
                //     for (Handler<MultiMap> modifier : modifiers) {
                //         modifier.handle(outgoingResponse.headers());
                //     }
                // }

                // Continue the interception chain
                return ctx.sendResponse();
            }
        });
    }

    /**
     * If the given header name is already contained in the request, this header will be used, otherwise the given header value is used.
     *
     * @param headerName  to check the request for
     * @param headerValue to use if the header name is not yet in the request
     * @param headers     of the request
     */
    protected void useOrSetHeader(String headerName, String headerValue, MultiMap headers) {
        if (headers.contains(headerName)) { // use
            LOGGER.debug("Using provided header '{}' with '{}'", headerName, headers.get(headerName));
        } else { // set
            headers.add(headerName, headerValue);
            LOGGER.debug("Set header '{}' to '{}'", headerName, headers.get(headerName));
        }
    }

    protected void addOrSetHeader(String headerName, String headerValue, MultiMap headers) {
        if (headers.contains(headerName)) { // add == append
            final String existingHeader = headers.get(headerName);
            headers.set(headerName, existingHeader + ", " + headerValue);
            LOGGER.debug("Appended to header '{}' to '{}' ", headerName, headers.get(headerName));
        } else { // set
            headers.add(headerName, headerValue);
            LOGGER.debug("Set header '{}' to '{}'", headerName, headers.get(headerName));
        }
    }

    private int portFromHostValue(String hostToParse, int defaultPort) {
        if (hostToParse == null) {
            return -1;
        } else {
            final int portSeparatorIdx = hostToParse.lastIndexOf(':');
            if (portSeparatorIdx > hostToParse.lastIndexOf(']')) {
                return parsePort(hostToParse.substring(portSeparatorIdx + 1), defaultPort);
            } else {
                return -1;
            }
        }
    }

    private int parsePort(String portToParse, int defaultPort) {
        try {
            return Integer.parseInt(portToParse);
        } catch (NumberFormatException ignored) {
            LOGGER.debug("Failed to parse a port from '{}'", portToParse);
            return defaultPort;
        }
    }

}
