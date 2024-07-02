package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.proxy.contextAware.ContextAwareHttpServerRequest;
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
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyRequest;
import io.vertx.httpproxy.ProxyResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxies requests and set the FORWARDED headers.
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling
 */
public class ProxyMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddleware.class);

    private static final CharSequence X_FORWARDED_PROTO = HttpHeaders.createOptimized("x-forwarded-proto");
    private static final CharSequence X_FORWARDED_PORT = HttpHeaders.createOptimized("x-forwarded-port");

    private static final String HTTPS = "https";

    private final HttpProxy httpProxy;

    private final String name;
    private final String serverHost;
    private final int serverPort;

    /**
    */
    public ProxyMiddleware(
        Vertx vertx,
        String name,
        String serverHost,
        int serverPort,
        String serverProtocol,
        Boolean httpsTrustAll,
        Boolean httpsVerifyHostname,
        String httpsTrustStorePath,
        String httpsTrustStorePassword
    ) {
        this.name = name;
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
                vertx));
        httpProxy.origin(serverPort, serverHost);

        lowercaseHostHeader(httpProxy);
        setHostHeader(httpProxy);
        setXForwardedHeaders(httpProxy);
        applyModifiers(httpProxy);
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

        LOGGER.debug("'{}' is proxying to '{}:{}{}'", name, serverHost, serverPort, ctx.request().uri());
        try {
            httpProxy.handle(new ContextAwareHttpServerRequest(ctx.request(), ctx));
        } catch (Exception e) {
            LOGGER.error("Error while proxying request", e);
            ctx.fail(e);
        }
    }

    /**
     * Due to a bug in the vertx-http-proxy comparing the host header case-sensitive with "host",
     * we have to make sure the incoming request host header is lowercase as well i.e. not "Host".
     * Otherwise, we have a weird combination of having a host header value in the request.headers()
     * and another host header value in the request.host.
     * Apparently, the host header value in request.headers() has presendence.
     * 
     * The bug was already fixed and is probably available in the next release.
     * This interceptor can then be removed.
     * See: https://github.com/eclipse-vertx/vertx-http-proxy/issues/77
     * 
     * @param proxy
     */
    protected void lowercaseHostHeader(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext proxyContext) {
                final ProxyRequest incomingRequest = proxyContext.request();
                if (incomingRequest.headers().contains("Host")) {
                    LOGGER.debug("lowercasing the host header name");
                    final String host = incomingRequest.headers().get("Host");
                    incomingRequest.headers().remove("Host");
                    incomingRequest.headers().set("host", host);
                }
                return proxyContext.sendRequest();
            }
        });
    }

    /**
     * 
     * Technically, vertx-http-proxy should set the host header to the target server.
     * However, due to a bug, it is set to a wrong value.
     * 
     * Once this issue has been fixed, this interceptor can be removed.
     * See: https://github.com/eclipse-vertx/vertx-http-proxy/issues/85
     * 
     * @param proxy
     */
    protected void setHostHeader(HttpProxy proxy) {
        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext proxyContext) {
                final ProxyRequest incomingRequest = proxyContext.request();
                LOGGER.debug("setting the host header to '{}:{}'", serverHost, serverPort);
                incomingRequest.setAuthority(HostAndPort.create(serverHost, serverPort));
                return proxyContext.sendRequest();
            }
        });
    }

    /**
     * Since version 4.3.5, the vertx-http-proxy sets the 'x-forwarded-host', in case it detects that
     * the outgoing request has a different host header value than the incoming request.
     * We previously set that header outself, alongside with 'x-forwarded-proto' and 'x-forwarded-port',
     * so we still need to set them here.
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
                LOGGER.debug("setting the '{}' header to '{}'", X_FORWARDED_PROTO, proto);
                useOrSetHeader(incomingRequest.headers(), X_FORWARDED_PROTO, proto);

                if (request.authority() != null) {
                    final int port = request.authority().port();
                    if (port > 0) {
                        LOGGER.debug("setting the '{}' header to '{}'", X_FORWARDED_PORT, port);
                        useOrSetHeader(incomingRequest.headers(), X_FORWARDED_PORT, String.valueOf(port));
                    }
                }

                return proxyContext.sendRequest();
            }
        });
    }

    /**
     * Generally, x-forwarded header should be extended. But apparently, hosts like keycloak refuse to serve
     * traffic that has 'http' in its 'x-forwarded-proto' header. As the portal-gateway is running behind and
     * ingress, that terminates TLS, the public protocol is always 'https', but the ingress talks plain 'http'
     * with the portal-gateway.
     * 
     * @param headers
     * @param headerName
     * @param headerValue
     */
    private void useOrSetHeader(MultiMap headers, CharSequence headerName, String headerValue) {
        if (headers.contains(headerName)) { // use
            LOGGER.debug("using provided header '{}' with '{}'", headerName, headers.get(headerName));
        } else { // set
            LOGGER.debug("setting header '{}' to '{}'", headerName, headers.get(headerName));
            headers.set(headerName, headerValue);
        }
    }

    /**
     * Generally, the requests or responses should be modified by the repective middleware itself.
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
                final List<Handler<StringBuilder>> modifiers = ctx.get(Middleware.REQUEST_URI_MODIFIERS);
                if (modifiers != null) {
                    if (modifiers.size() > 1) {
                        LOGGER.warn("Multiple URI modifiers declared: {} (total {})", modifiers, modifiers.size());
                    }
                    for (Handler<StringBuilder> modifier : modifiers) {
                        modifier.handle(uri);
                    }
                }
                incomingRequest.setURI(uri.toString());

                // continue the interception chain
                return proxyContext.sendRequest();
            }

            @Override
            public Future<Void> handleProxyResponse(ProxyContext proxyContext) {
                final RoutingContext ctx = getContextFromRequest(proxyContext);
                final ProxyResponse outgoingResponse = proxyContext.response();

                // modify headers
                final List<Handler<MultiMap>> modifiers = ctx.get(Middleware.RESPONSE_HEADERS_MODIFIERS);
                if (modifiers != null) {
                    for (Handler<MultiMap> modifier : modifiers) {
                        modifier.handle(outgoingResponse.headers());
                    }
                }

                // continue the interception chain
                return proxyContext.sendResponse();
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
