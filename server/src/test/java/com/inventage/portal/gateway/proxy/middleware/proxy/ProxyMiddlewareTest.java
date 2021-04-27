package com.inventage.portal.gateway.proxy.middleware.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventage.portal.gateway.TestUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class ProxyMiddlewareTest {

    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    @Test
    void proxyTest(Vertx vertx, VertxTestContext testCtx) {
        String host = "localhost";
        int proxyPort = TestUtils.findFreePort();
        int serverPort = TestUtils.findFreePort();
        String proxyResponse = "proxy";
        String serverResponse = "server";

        ProxyMiddleware proxy = new ProxyMiddleware(vertx, host, serverPort);

        Checkpoint proxyStarted = testCtx.checkpoint();
        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestProxied = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        Router router = Router.router(vertx);
        router.route().handler(proxy).handler(ctx -> ctx.response().end(proxyResponse));
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            requestProxied.flag();
        }).listen(proxyPort).onComplete(testCtx.succeeding(httpServer -> proxyStarted.flag()));

        vertx.createHttpServer().requestHandler(req -> {
            assertEquals(String.format("%s:%s", host, proxyPort), req.headers().get(X_FORWARDED_HOST));
            req.response().end(serverResponse);
            requestServed.flag();
        }).listen(serverPort).onComplete(testCtx.succeeding(httpServer -> serverStarted.flag()));

        vertx.createHttpClient().request(HttpMethod.GET, proxyPort, host, "/blub").compose(req -> req.send())
                .onComplete(testCtx.succeeding(resp -> {
                    testCtx.verify(() -> {
                        assertEquals(HttpResponseStatus.OK.code(), resp.statusCode());
                        responseReceived.flag();
                    });
                    resp.body().onComplete(testCtx.succeeding(body -> {
                        testCtx.verify(() -> {
                            assertEquals(serverResponse, body.toString());
                        });
                    }));
                }));
    }
}
