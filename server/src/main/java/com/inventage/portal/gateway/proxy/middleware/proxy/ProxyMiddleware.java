package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyRequest;
import io.vertx.httpproxy.ProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Proxies requests and set the FORWARDED headers.
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling
 */
public class ProxyMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddleware.class);

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

    private final HttpProxy httpProxy;

    private final String name;
    private final String serverHost;
    private final int serverPort;

    private List<Handler<StringBuilder>> incomingRequestURIModifiers;
    private List<Handler<MultiMap>> outgoingResponseHeadersModifiers;

    public ProxyMiddleware(Vertx vertx, String name, String serverHost, int serverPort) {
        this(vertx, name, "http", serverHost, serverPort);
    }

    public ProxyMiddleware(Vertx vertx, String name, String serverProtocol, String serverHost, int serverPort) {
        this.name = name;

        httpProxy = HttpProxy.reverseProxy(createHttpClient(serverProtocol, serverHost, serverPort, vertx));
        httpProxy.origin(serverPort, serverHost);
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        incomingRequestURIModifiers = new ArrayList<>();
        outgoingResponseHeadersModifiers = new ArrayList<>();
        applyModifiers(httpProxy);
    }

    protected HttpClient createHttpClient(String serverProtocol, String serverHost, int serverPort, Vertx vertx) {
        final HttpClientOptions options = new HttpClientOptions();
        if ("https".equalsIgnoreCase(serverProtocol)) {
            options.setSsl(true);
            options.setTrustAll(true);
            options.setVerifyHost(false);
            options.setLogActivity(LOGGER.isDebugEnabled());
            LOGGER.info("using HTTPS for host '{}'", serverHost);
        }
        return vertx.createHttpClient(options);
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        useOrSetHeader(X_FORWARDED_PROTO, ctx.request().scheme(), ctx.request().headers());
        useOrSetHeader(X_FORWARDED_HOST, ctx.request().host(), ctx.request().headers());
        useOrSetHeader(X_FORWARDED_PORT, String.valueOf(
                        portFromHostValue(
                                ctx.request().headers().get(X_FORWARDED_HOST),
                                portFromHostValue(ctx.request().host(), -1))),
                ctx.request().headers());
        ctx.request().headers().set(HttpHeaderNames.HOST, serverHost);
        captureModifiers(ctx);

        LOGGER.debug("'{}' is sending to '{}:{}{}'", name, serverHost, serverPort, ctx.request().uri());
        httpProxy.handle(ctx.request());
    }

    // capture the modifiers set by previous middlewares for use by the proxy interceptors
    protected void captureModifiers(RoutingContext ctx) {
        final List<Handler<StringBuilder>> requestURImodifiers = ctx.get(Middleware.REQUEST_URI_MODIFIERS);
        if (requestURImodifiers != null) {
            incomingRequestURIModifiers.addAll(requestURImodifiers);
        }

        final List<Handler<MultiMap>> responseHeadersModifiers = ctx.get(Middleware.RESPONSE_HEADERS_MODIFIERS);
        if (responseHeadersModifiers != null) {
            outgoingResponseHeadersModifiers.addAll(responseHeadersModifiers);
        }
    }

    protected void applyModifiers(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext ctx) {
                final ProxyRequest incomingRequest = ctx.request();

                // modify URI
                final StringBuilder uri = new StringBuilder(incomingRequest.getURI());
                final List<Handler<StringBuilder>> modifiers = incomingRequestURIModifiers;
                if (modifiers != null) {
                    if (modifiers.size() > 1) {
                        LOGGER.info("Multiple URI modifiers declared: {} (total {})", modifiers, modifiers.size());
                    }
                    for (Handler<StringBuilder> modifier : modifiers) {
                        modifier.handle(uri);
                    }
                }
                incomingRequest.setURI(uri.toString());

                incomingRequestURIModifiers = new ArrayList<>(); // reset

                // Continue the interception chain
                return ctx.sendRequest();
            }

            @Override
            public Future<Void> handleProxyResponse(ProxyContext ctx) {
                final ProxyResponse outgoingResponse = ctx.response();

                // modify headers
                final List<Handler<MultiMap>> modifiers = outgoingResponseHeadersModifiers;
                if (modifiers != null) {
                    for (Handler<MultiMap> modifier : modifiers) {
                        modifier.handle(outgoingResponse.headers());
                    }
                }

                outgoingResponseHeadersModifiers = new ArrayList<>(); // reset

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
        }
        else { // set
            headers.add(headerName, headerValue);
            LOGGER.debug("Set header '{}' to '{}'", headerName, headers.get(headerName));
        }
    }

    protected void addOrSetHeader(String headerName, String headerValue, MultiMap headers) {
        if (headers.contains(headerName)) { // add == append
            final String existingHeader = headers.get(headerName);
            headers.set(headerName, existingHeader + ", " + headerValue);
            LOGGER.debug("Appended to header '{}' to '{}' ", headerName, headers.get(headerName));
        }
        else { // set
            headers.add(headerName, headerValue);
            LOGGER.debug("Set header '{}' to '{}'", headerName, headers.get(headerName));
        }
    }

    private int portFromHostValue(String hostToParse, int defaultPort) {
        if (hostToParse == null) {
            return -1;
        }
        else {
            final int portSeparatorIdx = hostToParse.lastIndexOf(':');
            if (portSeparatorIdx > hostToParse.lastIndexOf(']')) {
                return parsePort(hostToParse.substring(portSeparatorIdx + 1), defaultPort);
            }
            else {
                return -1;
            }
        }
    }

    private int parsePort(String portToParse, int defaultPort) {
        try {
            return Integer.parseInt(portToParse);
        }
        catch (NumberFormatException ignored) {
            LOGGER.debug("Failed to parse a port from '{}'", portToParse);
            return defaultPort;
        }
    }

}
