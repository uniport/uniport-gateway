package com.inventage.portal.gateway.proxy.response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

public class ProxiedHttpServerResponse implements HttpServerResponse {

    private final HttpServerResponse delegate;

    public ProxiedHttpServerResponse(HttpServerResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
        return delegate.exceptionHandler(handler);
    }

    @Override
    public Future<Void> write(Buffer buffer) {
        return delegate.write(buffer);
    }

    @Override
    public void write(Buffer buffer, Handler<AsyncResult<Void>> handler) {
        delegate.write(buffer, handler);
    }

    @Override
    public HttpServerResponse setWriteQueueMaxSize(int i) {
        return delegate.setWriteQueueMaxSize(i);
    }

    @Override
    public boolean writeQueueFull() {
        return delegate.writeQueueFull();
    }

    @Override
    public HttpServerResponse drainHandler(Handler<Void> handler) {
        return delegate.drainHandler(handler);
    }

    @Override
    public int getStatusCode() {
        return delegate.getStatusCode();
    }

    @Override
    public HttpServerResponse setStatusCode(int i) {
        return delegate.setStatusCode(i);
    }

    @Override
    public String getStatusMessage() {
        return delegate.getStatusMessage();
    }

    @Override
    public HttpServerResponse setStatusMessage(String s) {
        return delegate.setStatusMessage(s);
    }

    @Override
    public HttpServerResponse setChunked(boolean b) {
        return delegate.setChunked(b);
    }

    @Override
    public boolean isChunked() {
        return delegate.isChunked();
    }

    @Override
    public MultiMap headers() {
        return delegate.headers();
    }

    @Override
    public HttpServerResponse putHeader(String s, String s1) {
        return delegate.putHeader(s, s1);
    }

    @Override
    public HttpServerResponse putHeader(CharSequence charSequence, CharSequence charSequence1) {
        return delegate.putHeader(charSequence, charSequence1);
    }

    @Override
    public HttpServerResponse putHeader(String s, Iterable<String> iterable) {
        return delegate.putHeader(s, iterable);
    }

    @Override
    public HttpServerResponse putHeader(CharSequence charSequence,
            Iterable<CharSequence> iterable) {
        return delegate.putHeader(charSequence, iterable);
    }

    @Override
    public MultiMap trailers() {
        return delegate.trailers();
    }

    @Override
    public HttpServerResponse putTrailer(String s, String s1) {
        return delegate.putTrailer(s, s1);
    }

    @Override
    public HttpServerResponse putTrailer(CharSequence charSequence, CharSequence charSequence1) {
        return delegate.putTrailer(charSequence, charSequence1);
    }

    @Override
    public HttpServerResponse putTrailer(String s, Iterable<String> iterable) {
        return delegate.putTrailer(s, iterable);
    }

    @Override
    public HttpServerResponse putTrailer(CharSequence charSequence,
            Iterable<CharSequence> iterable) {
        return delegate.putHeader(charSequence, iterable);
    }

    @Override
    public HttpServerResponse closeHandler(Handler<Void> handler) {
        return delegate.closeHandler(handler);
    }

    @Override
    public HttpServerResponse endHandler(Handler<Void> handler) {
        return delegate.endHandler(handler);
    }

    @Override
    public Future<Void> write(String s, String s1) {
        return delegate.write(s, s1);
    }

    @Override
    public void write(String s, String s1, Handler<AsyncResult<Void>> handler) {
        delegate.write(s, s1, handler);
    }

    @Override
    public Future<Void> write(String s) {
        return delegate.write(s);
    }

    @Override
    public void write(String s, Handler<AsyncResult<Void>> handler) {
        delegate.write(s, handler);
    }

    @Override
    public HttpServerResponse writeContinue() {
        return delegate.writeContinue();
    }

    @Override
    public Future<Void> end(String s) {
        return delegate.end(s);
    }

    @Override
    public void end(String s, Handler<AsyncResult<Void>> handler) {
        delegate.end(s, handler);
    }

    @Override
    public Future<Void> end(String s, String s1) {
        return delegate.end(s, s1);
    }

    @Override
    public void end(String s, String s1, Handler<AsyncResult<Void>> handler) {
        delegate.end(s, s1, handler);
    }

    @Override
    public Future<Void> end(Buffer buffer) {
        return delegate.end(buffer);
    }

    @Override
    public void end(Buffer buffer, Handler<AsyncResult<Void>> handler) {
        delegate.end(buffer, handler);
    }

    @Override
    public Future<Void> end() {
        return delegate.end();
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        delegate.end(handler);
    }

    @Override
    public Future<Void> sendFile(String s, long l, long l1) {
        return delegate.sendFile(s, l, l1);
    }

    @Override
    public HttpServerResponse sendFile(String s, long l, long l1,
            Handler<AsyncResult<Void>> handler) {
        return delegate.sendFile(s, l, l1, handler);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean ended() {
        return delegate.ended();
    }

    @Override
    public boolean closed() {
        return delegate.closed();
    }

    @Override
    public boolean headWritten() {
        return delegate.headWritten();
    }

    @Override
    public HttpServerResponse headersEndHandler(Handler<Void> handler) {
        return null;
    }

    @Override
    public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
        return delegate.bodyEndHandler(handler);
    }

    @Override
    public long bytesWritten() {
        return delegate.bytesWritten();
    }

    @Override
    public int streamId() {
        return delegate.streamId();
    }

    @Override
    public Future<HttpServerResponse> push(HttpMethod httpMethod, String s, String s1,
            MultiMap multiMap) {
        return delegate.push(httpMethod, s, s1, multiMap);
    }

    @Override
    public boolean reset(long l) {
        return delegate.reset(l);
    }

    @Override
    public HttpServerResponse writeCustomFrame(int i, int i1, Buffer buffer) {
        return delegate.writeCustomFrame(i, i1, buffer);
    }

    @Override
    public HttpServerResponse addCookie(Cookie cookie) {
        return delegate.addCookie(cookie);
    }

    @Override
    public Cookie removeCookie(String s, boolean b) {
        return delegate.removeCookie(s, b);
    }
}
