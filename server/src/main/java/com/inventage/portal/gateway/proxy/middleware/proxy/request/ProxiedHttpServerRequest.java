/*
 * Copyright 2014 Red Hat, Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Apache License v2.0 which accompanies this
 * distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

// NOTE: This code was copied from the io.vertx.ext.web.impl.HttpServerRequestWrapper
// https://github.com/vert-x3/vertx-web/blob/master/vertx-web/src/main/java/io/vertx/ext/web/impl/HttpServerRequestWrapper.java
package com.inventage.portal.gateway.proxy.middleware.proxy.request;

import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddleware;
import com.inventage.portal.gateway.proxy.middleware.proxy.response.ProxiedHttpServerResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ServerWebSocketWrapper;

/**
 * Subclass of HttpServerRequest which can be manipulated by various middleware functions. See
 * setXYZMiddleware() methods for manipulation possibilities.
 */
public class ProxiedHttpServerRequest implements HttpServerRequest {

    private final HttpServerRequest delegate;
    private final ForwardedParser forwardedParser;

    private UriMiddleware uriMiddleware;

    public ProxiedHttpServerRequest(RoutingContext rc, AllowForwardHeaders allowForward) {
        this.delegate = rc.request();
        this.forwardedParser = new ForwardedParser(delegate, allowForward);
    }

    public ProxiedHttpServerRequest setUriMiddleware(UriMiddleware middleware) {
        Objects.requireNonNull(middleware, "Given uri middleware must not be null!");
        this.uriMiddleware = middleware;
        return this;
    }

    @Override
    public HttpServerRequest body(Handler<AsyncResult<Buffer>> handler) {
        delegate.body(handler);
        return this;
    }

    @Override
    public Future<Buffer> body() {
        return delegate.body();
    }

    @Override
    public long bytesRead() {
        return delegate.bytesRead();
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        delegate.handler(handler);
        return this;
    }

    @Override
    public HttpServerRequest pause() {
        delegate.pause();
        return this;
    }

    @Override
    public HttpServerRequest resume() {
        delegate.resume();
        return this;
    }

    @Override
    public HttpServerRequest fetch(long amount) {
        delegate.fetch(amount);
        return this;
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> handler) {
        delegate.endHandler(handler);
        return this;
    }

    @Override
    public HttpVersion version() {
        return delegate.version();
    }

    @Override
    public HttpMethod method() {
        return delegate.method();
    }

    @Override
    public String uri() {
        if (this.uriMiddleware != null) {
            return uriMiddleware.apply(delegate.uri());
        }
        return delegate.uri();
    }

    @Override
    public String path() {
        return delegate.path();
    }

    @Override
    public String query() {
        return delegate.query();
    }

    @Override
    public MultiMap params() {
        return delegate.params();
    }

    @Override
    public String getParam(String param) {
        return delegate.getParam(param);
    }

    @Override
    public HttpServerResponse response() {
        return new ProxiedHttpServerResponse(delegate.response());
    }

    @Override
    public MultiMap headers() {
        return delegate.headers();
    }

    @Override
    public String getHeader(String s) {
        return delegate.getHeader(s);
    }

    @Override
    public String getHeader(CharSequence charSequence) {
        return delegate.getHeader(charSequence);
    }

    @Override
    public SocketAddress remoteAddress() {
        return forwardedParser.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return delegate.localAddress();
    }

    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
        return delegate.peerCertificateChain();
    }

    @Override
    public SSLSession sslSession() {
        return delegate.sslSession();
    }

    @Override
    public String absoluteURI() {
        return forwardedParser.absoluteURI();
    }

    @Override
    public String scheme() {
        return forwardedParser.scheme();
    }

    @Override
    public String host() {
        return forwardedParser.host();
    }

    @Override
    public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
        delegate.customFrameHandler(handler);
        return this;
    }

    @Override
    public HttpConnection connection() {
        return delegate.connection();
    }

    @Override
    public HttpServerRequest bodyHandler(Handler<Buffer> handler) {
        delegate.bodyHandler(handler);
        return this;
    }

    @Override
    public void toNetSocket(Handler<AsyncResult<NetSocket>> handler) {
        delegate.toNetSocket(handler);
    }

    @Override
    public Future<NetSocket> toNetSocket() {
        return delegate.toNetSocket();
    }

    @Override
    public HttpServerRequest setExpectMultipart(boolean b) {
        delegate.setExpectMultipart(b);
        return this;
    }

    @Override
    public boolean isExpectMultipart() {
        return delegate.isExpectMultipart();
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> handler) {
        delegate.uploadHandler(handler);
        return this;
    }

    @Override
    public MultiMap formAttributes() {
        return delegate.formAttributes();
    }

    @Override
    public String getFormAttribute(String s) {
        return delegate.getFormAttribute(s);
    }

    @Override
    public void toWebSocket(Handler<AsyncResult<ServerWebSocket>> handler) {
        delegate.toWebSocket(toWebSocket -> {
            if (toWebSocket.succeeded()) {
                handler.handle(Future.succeededFuture(new ServerWebSocketWrapper(
                        toWebSocket.result(), host(), scheme(), isSSL(), remoteAddress())));
            } else {
                handler.handle(toWebSocket);
            }
        });
    }

    @Override
    public Future<ServerWebSocket> toWebSocket() {
        return delegate.toWebSocket().map(
                ws -> new ServerWebSocketWrapper(ws, host(), scheme(), isSSL(), remoteAddress()));
    }

    @Override
    public boolean isEnded() {
        return delegate.isEnded();
    }

    @Override
    public boolean isSSL() {
        return forwardedParser.isSSL();
    }

    @Override
    public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
        delegate.streamPriorityHandler(handler);
        return this;
    }

    @Override
    public StreamPriority streamPriority() {
        return delegate.streamPriority();
    }

    @Override
    public Cookie getCookie(String name) {
        return delegate.getCookie(name);
    }

    @Override
    public int cookieCount() {
        return delegate.cookieCount();
    }

    @Override
    public Map<String, Cookie> cookieMap() {
        return delegate.cookieMap();
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        delegate.end(handler);
    }

    @Override
    public Future<Void> end() {
        return delegate.end();
    }

    @Override
    public HttpServerRequest routed(String route) {
        delegate.routed(route);
        return this;
    }

    @Override
    public Pipe<Buffer> pipe() {
        return delegate.pipe();
    }

    @Override
    public Future<Void> pipeTo(WriteStream<Buffer> dst) {
        return delegate.pipeTo(dst);
    }

    @Override
    public void pipeTo(WriteStream<Buffer> dst, Handler<AsyncResult<Void>> handler) {
        delegate.pipeTo(dst, handler);
    }
}
