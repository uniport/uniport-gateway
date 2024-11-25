package com.inventage.portal.gateway.proxy.middleware;

import com.inventage.portal.gateway.TestUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiddlewareServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiddlewareServer.class);

    private final Vertx vertx;
    private final VertxTestContext testCtx;
    private final HttpServer httpServer;
    private final String host;
    private int port;

    public MiddlewareServer(Vertx vertx, HttpServer httpServer, String host, VertxTestContext testCtx) {
        this.vertx = vertx;
        this.testCtx = testCtx;
        this.httpServer = httpServer;
        this.host = host;
    }

    public MiddlewareServer start() {
        final int port = TestUtils.findFreePort();
        return start(port);
    }

    public MiddlewareServer start(int port) {
        this.port = port;
        final Future<HttpServer> httpServerFuture = httpServer.listen(port, host);
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

    public void incomingRequest(HttpMethod method, String uri, Handler<HttpClientResponse> responseHandler) {
        incomingRequest(method, uri, new RequestOptions(), responseHandler);
    }

    public void incomingRequest(HttpMethod method, String uri, RequestOptions reqOpts, String body, Handler<HttpClientResponse> responseHandler) {
        reqOpts.setHost(host).setPort(port).setURI(uri).setMethod(method);
        createHttpClientWithRequestOptionsBodyAndResponseHandler(testCtx, reqOpts, body, responseHandler);
    }

    public void incomingRequest(HttpMethod method, String uri, RequestOptions reqOpts, Buffer body, Handler<HttpClientResponse> responseHandler) {
        reqOpts.setHost(host).setPort(port).setURI(uri).setMethod(method);
        createHttpClientWithRequestOptionsBufferBodyAndResponseHandler(testCtx, reqOpts, body, responseHandler);
    }

    public void incomingRequest(HttpMethod method, String uri, RequestOptions reqOpts, Handler<HttpClientResponse> responseHandler) {
        reqOpts.setHost(host).setPort(port).setURI(uri).setMethod(method);
        createHttpClientWithRequestOptionsAndResponseHandler(testCtx, reqOpts, responseHandler);
    }

    public void incomingRequestWithProxyRequestHandler(HttpMethod method, String uri, RequestOptions reqOpts, int proxyPort, Handler<HttpServerRequest> requestHandler) {
        reqOpts.setHost(host).setPort(port).setURI(uri).setMethod(method);
        vertx.createHttpServer().requestHandler(requestHandler).listen(proxyPort);

        createHttpClientWithRequestOptionsAndResponseHandler(testCtx, reqOpts, (responseHandler) -> {
        });
    }

    private void createHttpClientWithRequestOptionsAndResponseHandler(
        VertxTestContext testCtx, RequestOptions reqOpts,
        Handler<HttpClientResponse> responseHandler
    ) {
        LOGGER.info("requesting '{}'", reqOpts.getURI());
        vertx.createHttpClient()
            .request(reqOpts)
            .compose(HttpClientRequest::send)
            .onComplete(testCtx.succeeding(responseHandler));
    }

    private void createHttpClientWithRequestOptionsBodyAndResponseHandler(
        VertxTestContext testCtx, RequestOptions reqOpts, String body,
        Handler<HttpClientResponse> responseHandler
    ) {
        LOGGER.info("requesting '{}'", reqOpts.getURI());
        vertx.createHttpClient().request(reqOpts).compose(httpClientRequest -> httpClientRequest.send(body)).onComplete(testCtx.succeeding(responseHandler));
    }

    private void createHttpClientWithRequestOptionsBufferBodyAndResponseHandler(
        VertxTestContext testCtx, RequestOptions reqOpts, Buffer body,
        Handler<HttpClientResponse> responseHandler
    ) {
        LOGGER.info("requesting '{}'", reqOpts.getURI());
        vertx.createHttpClient().request(reqOpts).compose(httpClientRequest -> {
            return httpClientRequest.send(body);
        }).onComplete(testCtx.succeeding(responseHandler));
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
        return result.result();
    }

}
