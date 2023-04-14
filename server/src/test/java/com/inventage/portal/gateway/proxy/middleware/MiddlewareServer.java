package com.inventage.portal.gateway.proxy.middleware;

import com.inventage.portal.gateway.TestUtils;
import io.vertx.core.*;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiddlewareServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiddlewareServer.class);

    private final Vertx vertx;
    private final HttpServer httpServer;
    private final int port;
    private final String host;
    private final VertxTestContext testCtx;

    public MiddlewareServer(Vertx vertx, HttpServer httpServer, String host, VertxTestContext testCtx) {
        this.port = TestUtils.findFreePort();
        this.vertx = vertx;
        this.httpServer = httpServer;
        this.host = host;
        this.testCtx = testCtx;
    }

    public MiddlewareServer start() {
        Future<HttpServer> httpServerFuture = httpServer.listen(port, host);
        try {
            awaitComplete(httpServerFuture);
        } catch (Throwable e) {
            throw new RuntimeException("MiddlewareServer.start failed.", e);
        }
        return this;
    }

    public BrowserConnected connectBrowser() {
        return new BrowserConnected(this);
    }

    public void incomingRequest(HttpMethod method, String URI, Handler<HttpClientResponse> responseHandler) {
        incomingRequest(method, URI, new RequestOptions(), testCtx, responseHandler);
    }

    public void incomingRequest(HttpMethod method, String URI, RequestOptions reqOpts, Handler<HttpClientResponse> responseHandler) {
        incomingRequest(method, URI, reqOpts, testCtx, responseHandler);
    }

    public void incomingRequest(
            HttpMethod method,
            String URI,
            RequestOptions reqOpts,
            Handler<HttpClientResponse> responseHandler,
            MultiMap headers
    ) {
        reqOpts.setHost(host).setPort(port).setURI(URI).setMethod(method).setHeaders(headers);
        createHttpClientWithRequestOptionsAndResponseHandler(testCtx, reqOpts, responseHandler);
    }

    public void incomingRequest(HttpMethod method, String URI, VertxTestContext testCtx, Handler<HttpClientResponse> responseHandler) {
        incomingRequest(method, URI, new RequestOptions(), testCtx, responseHandler);
    }

    public void incomingRequest(HttpMethod method, String URI, RequestOptions reqOpts, VertxTestContext testCtx, Handler<HttpClientResponse> responseHandler) {
        reqOpts.setHost(host).setPort(port).setURI(URI).setMethod(method);
        createHttpClientWithRequestOptionsAndResponseHandler(testCtx, reqOpts, responseHandler);
    }

    public void incomingRequest(
            HttpMethod method,
            String URI,
            RequestOptions reqOpts,
            VertxTestContext testCtx,
            Handler<HttpClientResponse> responseHandler,
            MultiMap headers
    ) {
        reqOpts.setHost(host).setPort(port).setURI(URI).setMethod(method).setHeaders(headers);
        createHttpClientWithRequestOptionsAndResponseHandler(testCtx, reqOpts, responseHandler);
    }

    private void createHttpClientWithRequestOptionsAndResponseHandler(VertxTestContext testCtx, RequestOptions reqOpts,
        Handler<HttpClientResponse> responseHandler) {
        LOGGER.info("requesting '{}'", reqOpts.getURI());
        vertx.createHttpClient().request(reqOpts).compose(HttpClientRequest::send).onComplete(testCtx.succeeding(resp -> {
            responseHandler.handle(resp);
        }));
    }

    // wait until Vert.x is listening to prevent test failures because of not open ports
    private <T> T awaitComplete(Future<T> f) throws Throwable {
        final Object lock = new Object();
        final AtomicReference<AsyncResult<T>> resultRef = new AtomicReference<>(null);
        synchronized (lock) {
            // We *must* be locked before registering a callback.
            // If result is ready, the callback is called immediately!
            f.onComplete((AsyncResult<T> result) -> {
                resultRef.set(result);
                synchronized (lock) {
                    lock.notify();
                }
            });

            do {
                // Nested sync on lock is fine.  If we get a spurious wake-up before resultRef is set, we need to
                // reacquire the lock, then wait again.
                // Ref: https://stackoverflow.com/a/249907/257299
                synchronized (lock) {
                    // @Blocking
                    lock.wait();
                }
            } while (null == resultRef.get());
        }
        final AsyncResult<T> result = resultRef.get();
        final Throwable t = result.cause();
        if (null != t) {
            throw t;
        }
        final T x = result.result();
        return x;
    }

}
