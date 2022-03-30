package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxTestContext;

public class MiddlewareServer {

    private final Vertx vertx;
    private HttpServer httpServer;
    private final int port;

    public MiddlewareServer(Vertx vertx, HttpServer httpServer, int port) {
        this.port = port;
        this.httpServer = httpServer;
        this.vertx = vertx;
    }

    public void doRequest(VertxTestContext testCtx, RequestOptions reqOpts, Handler<HttpClientResponse> responseHandler) {
        reqOpts.setHost("localhost").setPort(port).setURI("/").setMethod(HttpMethod.GET);
        vertx.createHttpClient().request(reqOpts).compose(req -> req.send()).onComplete(testCtx.succeeding(resp -> {
            responseHandler.handle(resp);
            httpServer.close();
        }));
    }

}
