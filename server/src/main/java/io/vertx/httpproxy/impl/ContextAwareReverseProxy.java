/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.httpproxy.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyOptions;
import io.vertx.httpproxy.ProxyRequest;
import io.vertx.httpproxy.ProxyResponse;
import io.vertx.httpproxy.cache.CacheOptions;
import io.vertx.httpproxy.spi.cache.Cache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * This class is copied from:
 * https://github.com/eclipse-vertx/vertx-http-proxy/blob/4.4.4/src/main/java/io/vertx/httpproxy/impl/ReverseProxy.java
 *
 * The following changes were made:
 * - handles() accepts RoutingContext instead of HttpServerRequest
 * 
 * @see ReverseProxy
 */
public class ContextAwareReverseProxy implements HttpProxy {

    public static final String CONTEXT_AWARE_REVERSE_PROXY_KEY = "uniport.contextAwareReverseProxy";

    private final HttpClient client;
    private final boolean supportWebSocket;
    private BiFunction<HttpServerRequest, HttpClient, Future<HttpClientRequest>> selector = (req, client) -> Future.failedFuture("No origin available");
    private final List<ProxyInterceptor> interceptors = new ArrayList<>();

    public ContextAwareReverseProxy(ProxyOptions options, HttpClient client) {
        final CacheOptions cacheOptions = options.getCacheOptions();
        if (cacheOptions != null) {
            final Cache<String, Resource> cache = cacheOptions.newCache();
            addInterceptor(new CachingFilter(cache));
        }
        this.client = client;
        this.supportWebSocket = options.getSupportWebSocket();
    }

    @Override
    public HttpProxy originRequestProvider(BiFunction<HttpServerRequest, HttpClient, Future<HttpClientRequest>> provider) {
        selector = provider;
        return this;
    }

    @Override
    public HttpProxy addInterceptor(ProxyInterceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    public void handle(RoutingContext ctx) {
        handle(ctx, ctx.request());
    }

    @Override
    public void handle(HttpServerRequest request) {
        handle(null, request);
    }

    private void handle(RoutingContext ctx, HttpServerRequest request) {
        final ProxyRequest proxyRequest = ProxyRequest.reverseProxy(request);

        // Encoding sanity check
        final Boolean chunked = HttpUtils.isChunked(request.headers());
        if (chunked == null) {
            end(proxyRequest, 400);
            return;
        }

        // WebSocket upgrade tunneling
        if (supportWebSocket &&
            request.version() == HttpVersion.HTTP_1_1 &&
            request.method() == HttpMethod.GET &&
            request.headers().contains(HttpHeaders.CONNECTION, HttpHeaders.UPGRADE, true)) {
            handleWebSocketUpgrade(proxyRequest);
            return;
        }

        final Proxy proxy = new Proxy(proxyRequest);
        proxy.set(CONTEXT_AWARE_REVERSE_PROXY_KEY, ctx);
        proxy.filters = interceptors.listIterator();
        proxy.sendRequest().compose(proxy::sendProxyResponse);
    }

    private void handleWebSocketUpgrade(ProxyRequest proxyRequest) {
        final HttpServerRequest proxiedRequest = proxyRequest.proxiedRequest();
        resolveOrigin(proxiedRequest).onComplete(ar -> {
            if (ar.succeeded()) {
                final HttpClientRequest request = ar.result();
                request.setMethod(HttpMethod.GET);
                request.setURI(proxiedRequest.uri());
                request.headers().addAll(proxiedRequest.headers());
                final Future<HttpClientResponse> fut2 = request.connect();
                proxiedRequest.handler(request::write);
                proxiedRequest.endHandler(v -> request.end());
                proxiedRequest.resume();
                fut2.onComplete(ar2 -> {
                    if (ar2.succeeded()) {
                        final HttpClientResponse proxiedResponse = ar2.result();
                        if (proxiedResponse.statusCode() == 101) {
                            final HttpServerResponse response = proxiedRequest.response();
                            response.setStatusCode(101);
                            response.headers().addAll(proxiedResponse.headers());
                            final Future<NetSocket> otherso = proxiedRequest.toNetSocket();
                            otherso.onComplete(ar3 -> {
                                if (ar3.succeeded()) {
                                    final NetSocket responseSocket = ar3.result();
                                    final NetSocket proxyResponseSocket = proxiedResponse.netSocket();
                                    responseSocket.handler(proxyResponseSocket::write);
                                    proxyResponseSocket.handler(responseSocket::write);
                                    responseSocket.closeHandler(v -> proxyResponseSocket.close());
                                    proxyResponseSocket.closeHandler(v -> responseSocket.close());
                                } else {
                                    // Find reproducer
                                    System.err.println("Handle this case");
                                    ar3.cause().printStackTrace();
                                }
                            });
                        } else {
                            // Rejection
                            proxiedRequest.resume();
                            end(proxyRequest, proxiedResponse.statusCode());
                        }
                    } else {
                        proxiedRequest.resume();
                        end(proxyRequest, 502);
                    }
                });
            } else {
                proxiedRequest.resume();
                end(proxyRequest, 502);
            }
        });
    }

    private void end(ProxyRequest proxyRequest, int sc) {
        proxyRequest
            .response()
            .release()
            .setStatusCode(sc)
            .putHeader(HttpHeaders.CONTENT_LENGTH, "0")
            .setBody(null)
            .send();
    }

    private Future<HttpClientRequest> resolveOrigin(HttpServerRequest proxiedRequest) {
        return selector.apply(proxiedRequest, client);
    }

    private final class Proxy implements ProxyContext {

        private final ProxyRequest request;
        private ProxyResponse response;
        private final Map<String, Object> attachments = new HashMap<>();
        private ListIterator<ProxyInterceptor> filters;

        private Proxy(ProxyRequest request) {
            this.request = request;
        }

        @Override
        public void set(String name, Object value) {
            attachments.put(name, value);
        }

        @Override
        public <T> T get(String name, Class<T> type) {
            final Object o = attachments.get(name);
            return type.isInstance(o) ? type.cast(o) : null;
        }

        @Override
        public ProxyRequest request() {
            return request;
        }

        @Override
        public Future<ProxyResponse> sendRequest() {
            if (filters.hasNext()) {
                final ProxyInterceptor next = filters.next();
                return next.handleProxyRequest(this);
            } else {
                return sendProxyRequest(request);
            }
        }

        @Override
        public ProxyResponse response() {
            return response;
        }

        @Override
        public Future<Void> sendResponse() {
            if (filters.hasPrevious()) {
                final ProxyInterceptor filter = filters.previous();
                return filter.handleProxyResponse(this);
            } else {
                return response.send();
            }
        }

        private Future<ProxyResponse> sendProxyRequest(ProxyRequest proxyRequest) {
            final Future<HttpClientRequest> f = resolveOrigin(proxyRequest.proxiedRequest());
            f.onFailure(err -> {
                // Should this be done here ? I don't think so
                final HttpServerRequest proxiedRequest = proxyRequest.proxiedRequest();
                proxiedRequest.resume();
                final Promise<Void> promise = Promise.promise();
                proxiedRequest.exceptionHandler(promise::tryFail);
                proxiedRequest.endHandler(promise::tryComplete);
                promise.future().onComplete(ar2 -> {
                    end(proxyRequest, 502);
                });
            });
            return f.compose(a -> sendProxyRequest(proxyRequest, a));
        }

        private Future<ProxyResponse> sendProxyRequest(ProxyRequest proxyRequest, HttpClientRequest request) {
            final Future<ProxyResponse> fut = proxyRequest.send(request);
            fut.onFailure(err -> {
                proxyRequest.proxiedRequest().response().setStatusCode(502).end();
            });
            return fut;
        }

        private Future<Void> sendProxyResponse(ProxyResponse response) {

            this.response = response;

            // Check validity
            final Boolean chunked = HttpUtils.isChunked(response.headers());
            if (chunked == null) {
                // response.request().release(); // Is it needed ???
                end(response.request(), 501);
                return Future.succeededFuture(); // should use END future here ???
            }

            return sendResponse();
        }
    }
}
