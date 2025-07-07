package com.inventage.portal.gateway.proxy.service;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.service.contextAware.ContextAwareHttpServerRequest;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyRequest;
import io.vertx.httpproxy.ProxyResponse;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxies requests and set the FORWARDED headers.
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling
 */
public class ReverseProxy extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReverseProxy.class);

    private static final CharSequence X_FORWARDED_PROTO = HttpHeaders.createOptimized("x-forwarded-proto");
    private static final CharSequence X_FORWARDED_HOST = HttpHeaders.createOptimized("x-forwarded-host");
    private static final CharSequence X_FORWARDED_PORT = HttpHeaders.createOptimized("x-forwarded-port");
    private static final CharSequence X_FORWARDED_FOR = HttpHeaders.createOptimized("x-forwarded-for");

    private static final String HTTPS = "https";

    private final HttpProxy httpProxy;

    private final String name;

    private final String serverProto;
    private final String serverHost;
    private final int serverPort;

    public ReverseProxy(
        Vertx vertx,
        String name,
        String serverHost,
        int serverPort,
        String serverProtocol,
        Boolean httpsTrustAll,
        Boolean httpsVerifyHostname,
        String httpsTrustStorePath,
        String httpsTrustStorePassword,
        boolean verbose
    ) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(serverHost, "serverHost must not be null");
        Objects.requireNonNull(serverProtocol, "serverProtocol must not be null");
        Objects.requireNonNull(httpsTrustAll, "httpsTrustAll must not be null");
        Objects.requireNonNull(httpsVerifyHostname, "httpsVerifyHostname must not be null");
        // httpsTrustStorePath is allowed to be null
        // httpsTrustStorePassword is allowed to be null

        this.name = name;
        this.serverProto = serverProtocol;
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        httpProxy = HttpProxy.reverseProxy(
            createHttpClient(
                serverProtocol,
                serverHost,
                httpsTrustAll,
                httpsVerifyHostname,
                httpsTrustStorePath,
                httpsTrustStorePassword,
                vertx))
            .origin(serverPort, serverHost);

        setXForwardedHeaders(httpProxy);
        applyModifiers(httpProxy);
        if (verbose) {
            logRequestResponse(httpProxy);
        }
    }

    protected HttpClient createHttpClient(
        String serverProtocol,
        String serverHost,
        Boolean httpsTrustAll,
        Boolean httpsVerifyHostname,
        String httpsTrustStorePath,
        String httpsTrustStorePassword,
        Vertx vertx
    ) {
        final HttpClientOptions options = new HttpClientOptions();
        if (HTTPS.equalsIgnoreCase(serverProtocol)) {
            options.setSsl(true);
            options.setTrustAll(httpsTrustAll);
            options.setVerifyHost(httpsVerifyHostname);
            if (httpsTrustStorePath != null && httpsTrustStorePassword != null) {
                options.setTrustOptions(
                    new JksOptions()
                        .setPath(httpsTrustStorePath)
                        .setPassword(httpsTrustStorePassword));
            }
            options.setLogActivity(LOGGER.isDebugEnabled());
            LOGGER.info("using HTTPS for host '{}'", serverHost);
        }

        return vertx.createHttpClient(options);
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        LOGGER.debug("'{}' is proxying to '{}://{}:{}{}'", name, serverProto, serverHost, serverPort, ctx.request().uri());
        try {
            httpProxy.handle(new ContextAwareHttpServerRequest(ctx.request(), ctx));
        } catch (Exception e) {
            LOGGER.error("Error while proxying request", e);
            ctx.fail(e);
        }
    }

    /**
     * Since version 4.3.5, the vertx-http-proxy sets the 'x-forwarded-host', in case it detects that
     * the outgoing request has a different host header value than the incoming request.
     * However, since version 4.5.15, it only checks if the authority was modified in an interceptor, so
     * again we need to set it ourself.
     * 
     * We also set 'x-forwarded-proto' and 'x-forwarded-port'.
     * 
     * See https://github.com/eclipse-vertx/vertx-http-proxy/issues/130
     * 
     * @param proxy
     */
    protected void setXForwardedHeaders(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext proxyContext) {
                final ProxyRequest incomingRequest = proxyContext.request();
                final HttpServerRequest request = incomingRequest.proxiedRequest();

                final String proto = request.scheme();
                useOrSetHeader(incomingRequest.headers(), X_FORWARDED_PROTO, proto);

                final HostAndPort authority = request.authority();
                if (authority != null) {
                    useOrSetHeader(incomingRequest.headers(), X_FORWARDED_HOST, authority.toString());

                    final int port = authority.port();
                    if (port > 0) {
                        useOrSetHeader(incomingRequest.headers(), X_FORWARDED_PORT, String.valueOf(port));
                    }

                }

                final SocketAddress remoteAddress = request.remoteAddress();
                if (remoteAddress != null) {
                    incomingRequest.headers().add(X_FORWARDED_FOR, remoteAddress.toString());
                }

                return proxyContext.sendRequest();
            }
        });
    }

    /**
     * Generally, x-forwarded headers should only be added, if they are not yet present on the request.
     * One exception is x-forwarded-for, that should be extended by each intermediate proxy.
     * 
     * See
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Forwarded-Proto
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Forwarded-Host
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Forwarded-For
     * 
     * @param headers
     * @param headerName
     * @param headerValue
     */
    private void useOrSetHeader(MultiMap headers, CharSequence headerName, String headerValue) {
        if (headers.contains(headerName)) {
            LOGGER.debug("using provided header '{}' with '{}'", headerName, headers.get(headerName));
            return;
        }
        LOGGER.debug("setting header '{}' to '{}'", headerName, headerValue);
        headers.set(headerName, headerValue);
    }

    /**
     * Generally, the requests or responses should be modified by the respective middleware itself.
     * However, some operations are not possible on a HttpServerRequest, such as changing the request's path.
     * Similarly for some operations on the response, like operations on response headers.
     * To circumvent this, we do that here in the proxy middleware.
     * 
     * @param proxy
     */
    protected void applyModifiers(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext proxyContext) {
                final ProxyRequest incomingRequest = proxyContext.request();
                final RoutingContext ctx = getContextFromRequest(proxyContext);

                // modify URI
                final StringBuilder uri = new StringBuilder(incomingRequest.getURI());
                final List<Handler<StringBuilder>> uriModifiers = ctx.get(Middleware.REQUEST_URI_MODIFIERS);
                if (uriModifiers != null) {
                    if (uriModifiers.size() > 1) {
                        LOGGER.warn("Multiple URI modifiers declared: {} (total {})", uriModifiers, uriModifiers.size());
                    }
                    for (Handler<StringBuilder> modifier : uriModifiers) {
                        modifier.handle(uri);
                    }
                }
                incomingRequest.setURI(uri.toString());

                // modify headers
                final List<Handler<MultiMap>> headerModifiers = ctx.get(Middleware.REQUEST_HEADERS_MODIFIERS);
                if (headerModifiers != null) {
                    for (Handler<MultiMap> modifier : headerModifiers) {
                        modifier.handle(incomingRequest.headers());
                    }
                }

                // continue the interception chain
                return proxyContext.sendRequest();
            }
        });
    }

    protected void logRequestResponse(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext proxyContext) {
                final ProxyRequest outgoingRequest = proxyContext.request();

                final String version = outgoingRequest.version().alpnName();
                final String method = outgoingRequest.getMethod().name();
                final String uri = outgoingRequest.getURI();
                final String authority = outgoingRequest.proxiedRequest().authority().toString();
                final String headers = headersToString(outgoingRequest.headers());

                log(String.format("%s %s %s - Host: %s - %s",
                    version,
                    method,
                    uri,
                    authority,
                    headers));

                return proxyContext.sendRequest();
            }

            @Override
            public Future<Void> handleProxyResponse(ProxyContext proxyContext) {
                final ProxyResponse incomingResponse = proxyContext.response();

                final int statusCode = incomingResponse.getStatusCode();
                final String statusMessage = incomingResponse.getStatusMessage();
                final String headers = headersToString(incomingResponse.headers());

                log(String.format("%d %s - %s",
                    statusCode,
                    statusMessage,
                    headers));

                return proxyContext.sendResponse();
            }

            private String headersToString(MultiMap headers) {
                final StringBuilder headerBuilder = new StringBuilder();
                for (Entry<String, String> nameValue : headers.entries()) {
                    headerBuilder.append(nameValue.getKey())
                        .append(": ")
                        .append(nameValue.getValue())
                        .append(" - ");
                }
                return headerBuilder.toString();
            }

            private void log(String message) {
                if (LOGGER.isTraceEnabled() || LOGGER.isDebugEnabled()) {
                    LOGGER.debug(message);
                } else if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(message);
                }
            }
        });
    }

    /**
     * Before handing the request to the vertx-http-proxy, we wrapped in into {@code ContextAwareHttpServerRequest} to access
     * the {@code RoutingContext} in the vertx-http-proxy interceptors.
     * So we need to unwrap it again.
     * 
     * @param proxyContext
     * @return
     */
    private RoutingContext getContextFromRequest(ProxyContext proxyContext) {
        final ProxyRequest incomingRequest = proxyContext.request();
        if (!(incomingRequest.proxiedRequest() instanceof ContextAwareHttpServerRequest)) {
            final String errMsg = "request has to be of type ContextAwareHttpServerRequest";
            LOGGER.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        final ContextAwareHttpServerRequest contextAwareRequest = (ContextAwareHttpServerRequest) incomingRequest.proxiedRequest();
        return contextAwareRequest.routingContext();
    }
}
