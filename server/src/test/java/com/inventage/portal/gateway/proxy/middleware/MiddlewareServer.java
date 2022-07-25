package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.junit5.VertxTestContext;

public class MiddlewareServer {

    private final Vertx vertx;
    private HttpServer httpServer;
    private final int port;
    private final String host;

    public MiddlewareServer(Vertx vertx, HttpServer httpServer, int port, String host) {
        this.port = port;
        this.httpServer = httpServer;
        this.vertx = vertx;
        this.host = host;
    }

    public void incomingRequest(VertxTestContext testCtx, RequestOptions reqOpts, Handler<HttpClientResponse> responseHandler) {
        reqOpts.setHost(host).setPort(port).setURI("/").setMethod(HttpMethod.GET);
        httpServer.listen(port, host).onComplete(testCtx.succeeding(p -> {
            createHttpClientWithRequestOptionsAndResponseHandler(testCtx, reqOpts, responseHandler);
        }));
    }

    private void createHttpClientWithRequestOptionsAndResponseHandler(VertxTestContext testCtx, RequestOptions reqOpts,
                                                                      Handler<HttpClientResponse> responseHandler) {
        vertx.createHttpClient().request(reqOpts).compose(HttpClientRequest::send).onComplete(testCtx.succeeding(resp -> {
            responseHandler.handle(resp);
        }));
    }
}
