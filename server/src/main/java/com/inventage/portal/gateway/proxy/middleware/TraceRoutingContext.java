package com.inventage.portal.gateway.proxy.middleware;

import io.opentelemetry.api.trace.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.impl.RoutingContextInternal;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wraps a {@link RoutingContext} and simply delegate all method calls to the wrapped handler
 * See: https://github.com/vert-x3/vertx-web/blob/4.5.8/vertx-web/src/main/java/io/vertx/ext/web/impl/RoutingContextInternal.java
 * 
 * Additionally, any call finishing the current handler, ends the span.
 *
 */
public class TraceRoutingContext implements RoutingContextInternal {

    private final RoutingContextInternal ctx;
    private final Span span;

    public TraceRoutingContext(RoutingContext ctx, Span span) {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(span);
        this.ctx = (RoutingContextInternal) ctx;
        this.span = span;
    }

    @Override
    public void fail(int statusCode) {
        span.end();
        ctx.fail(statusCode);
    }

    @Override
    public void fail(Throwable throwable) {
        span.end();
        ctx.fail(throwable);
    }

    @Override
    public void fail(int statusCode, Throwable throwable) {
        span.end();
        ctx.fail(statusCode, throwable);
    }

    @Override
    public void next() {
        span.end();
        ctx.next();
    }

    @Override
    public Future<Void> redirect(String url) {
        span.end();
        return ctx.redirect(url);
    }

    @Override
    public void reroute(HttpMethod method, String path) {
        span.end();
        ctx.reroute(method, path);
    }

    @Override
    public Future<Void> end() {
        span.end();
        return this.ctx.end();
    }

    @Override
    public Future<Void> end(String chunk) {
        span.end();
        return this.ctx.end(chunk);
    }

    @Override
    public RoutingContextInternal visitHandler(int id) {
        return ctx.visitHandler(id);
    }

    @Override
    public boolean seenHandler(int id) {
        return ctx.seenHandler(id);
    }

    @Override
    public RoutingContextInternal setMatchFailure(int matchFailure) {
        return ctx.setMatchFailure(matchFailure);
    }

    @Override
    public int addBodyEndHandler(Handler<Void> handler) {
        return ctx.addBodyEndHandler(handler);
    }

    @Override
    @Deprecated
    public RoutingContext addCookie(io.vertx.core.http.Cookie cookie) {
        return ctx.addCookie(cookie);
    }

    @Override
    public int addEndHandler(Handler<AsyncResult<Void>> handler) {
        return ctx.addEndHandler(handler);
    }

    @Override
    public int addHeadersEndHandler(Handler<Void> handler) {
        return ctx.addHeadersEndHandler(handler);
    }

    @Override
    @Deprecated
    public int cookieCount() {
        return ctx.cookieCount();
    }

    @Override
    @Deprecated
    public Map<String, io.vertx.core.http.Cookie> cookieMap() {
        return ctx.cookieMap();
    }

    @Override
    public Route currentRoute() {
        return ctx.currentRoute();
    }

    @Override
    public Router currentRouter() {
        return ctx.currentRouter();
    }

    @Override
    public RoutingContextInternal parent() {
        return ctx.parent();
    }

    @Override
    public Map<String, Object> data() {
        return ctx.data();
    }

    @Override
    public boolean failed() {
        return ctx.failed();
    }

    @Override
    public Throwable failure() {
        return ctx.failure();
    }

    @Override
    public List<FileUpload> fileUploads() {
        return ctx.fileUploads();
    }

    @Override
    public void cancelAndCleanupFileUploads() {
        ctx.cancelAndCleanupFileUploads();
    }

    @Override
    public <T> T get(String key) {
        return ctx.get(key);
    }

    @Override
    public <T> T get(String key, T defaultValue) {
        return ctx.get(key, defaultValue);
    }

    @Override
    public <T> T remove(String key) {
        return ctx.remove(key);
    }

    @Override
    public String getAcceptableContentType() {
        return ctx.getAcceptableContentType();
    }

    @Override
    public RequestBody body() {
        return ctx.body();
    }

    @Override
    @Deprecated
    public Cookie getCookie(String name) {
        return ctx.getCookie(name);
    }

    @Override
    public String mountPoint() {
        return ctx.mountPoint();
    }

    @Override
    public String normalizedPath() {
        return ctx.normalizedPath();
    }

    @Override
    public RoutingContext put(String key, Object obj) {
        return ctx.put(key, obj);
    }

    @Override
    public boolean removeBodyEndHandler(int handlerID) {
        return ctx.removeBodyEndHandler(handlerID);
    }

    @Override
    @Deprecated
    public Cookie removeCookie(String name, boolean invalidate) {
        return ctx.removeCookie(name, invalidate);
    }

    @Override
    public boolean removeEndHandler(int handlerID) {
        return ctx.removeEndHandler(handlerID);
    }

    @Override
    public boolean removeHeadersEndHandler(int handlerID) {
        return ctx.removeHeadersEndHandler(handlerID);
    }

    @Override
    public HttpServerRequest request() {
        return ctx.request();
    }

    @Override
    public HttpServerResponse response() {
        return ctx.response();
    }

    @Override
    public User user() {
        return ctx.user();
    }

    @Override
    public Session session() {
        return ctx.session();
    }

    @Override
    public boolean isSessionAccessed() {
        return ctx.isSessionAccessed();
    }

    @Override
    public ParsedHeaderValues parsedHeaders() {
        return ctx.parsedHeaders();
    }

    @Override
    public void setAcceptableContentType(String contentType) {
        ctx.setAcceptableContentType(contentType);
    }

    @Override
    public Map<String, String> pathParams() {
        return ctx.pathParams();
    }

    @Override
    public String pathParam(String name) {
        return ctx.pathParam(name);
    }

    @Override
    public MultiMap queryParams() {
        return ctx.queryParams();
    }

    @Override
    public MultiMap queryParams(Charset charset) {
        return ctx.queryParams(charset);
    }

    @Override
    public List<String> queryParam(String query) {
        return ctx.queryParam(query);
    }

    @Override
    @Deprecated
    public void setBody(Buffer body) {
        ctx.setBody(body);
    }

    @Override
    @Deprecated
    public void setSession(Session session) {
        ctx.setSession(session);
    }

    @Override
    public int restIndex() {
        return ctx.restIndex();
    }

    @Override
    public boolean normalizedMatch() {
        return ctx.normalizedMatch();
    }

    @Override
    public void setUser(User user) {
        ctx.setUser(user);
    }

    @Override
    public void clearUser() {
        ctx.clearUser();
    }

    @Override
    public int statusCode() {
        return ctx.statusCode();
    }

    @Override
    public Vertx vertx() {
        return ctx.vertx();
    }
}
