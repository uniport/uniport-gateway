package ch.uniport.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
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
        // Bind port 0 and read the real port back. Picking a free port up front and binding it
        // later is racy: between the two steps another test can take the port, which shows up as
        // "Address already in use" or "Connection reset".
        try {
            this.port = httpServer.listen(0, host)
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS)
                .actualPort();
        } catch (Exception e) {
            throw new RuntimeException("MiddlewareServer.start failed.", e);
        }
        return this;
    }

    public int port() {
        return port;
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

}
