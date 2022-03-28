package com.inventage.portal.gateway.proxy.middleware.cors;

import com.inventage.portal.gateway.TestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;

import static io.vertx.core.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class CorsMiddlewareTest {

    private int port = TestUtils.findFreePort();

    @Test
    public void test_GET_no_origin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGatewayWithCorsMiddleware(vertx, "http://portal.minikube");
        // when
        doRequest(vertx, testCtx, new RequestOptions(), (resp) -> {
            // then
            assertEquals(200, resp.statusCode(), "unexpected status code");
            assertEquals("origin", resp.getHeader(VARY));
            testCtx.completeNow();
        });
    }

    @Test
    public void test_GET_origin_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGatewayWithCorsMiddleware(vertx, "http://portal.minikube");
        // when
        doRequest(vertx, testCtx, new RequestOptions().addHeader(ORIGIN, "http://portal.minikube"), (resp) -> {
            // then
            assertEquals(200, resp.statusCode(), "unexpected status code");
            assertEquals("http://portal.minikube", resp.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
            assertEquals("origin", resp.getHeader(VARY));
            testCtx.completeNow();
        });
    }

    @Test
    public void test_GET_all_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGatewayWithCorsMiddleware(vertx, "*");
        // when
        doRequest(vertx, testCtx, new RequestOptions().addHeader(ORIGIN, "http://other.com"), (resp) -> {
            // then
            assertEquals(200, resp.statusCode(), "unexpected status code");
            assertEquals("*", resp.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
            assertEquals("origin", resp.getHeader(VARY));
            testCtx.completeNow();
        });
    }

    @Test
    public void test_GET_origin_not_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGatewayWithCorsMiddleware(vertx, "http://portal.minikube");
        // when
        doRequest(vertx, testCtx, new RequestOptions().addHeader(ORIGIN, "http://bad.com"), (resp) -> {
            // then
            assertEquals(403, resp.statusCode(), "unexpected status code");
            testCtx.completeNow();
        });
    }

    @Test
    public void test_OPTIONS_no_origin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGatewayWithCorsMiddleware(vertx, "http://portal.minikube");
        // when
        doRequest(vertx, testCtx, new RequestOptions(), (resp) -> {
            // then
            assertEquals(200, resp.statusCode(), "unexpected status code");
            assertEquals("origin", resp.getHeader(VARY));
            testCtx.completeNow();
        });
    }



    void doRequest(Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts, Handler<HttpClientResponse> responseHandler) {
        reqOpts.setHost("localhost").setPort(port).setURI("/").setMethod(HttpMethod.GET);
        vertx.createHttpClient().request(reqOpts).compose(req -> req.send()).onComplete(testCtx.succeeding(resp -> {
            responseHandler.handle(resp);
        }));
    }

    private void portalGatewayWithCorsMiddleware(Vertx vertx, String allowedOrigin) throws InterruptedException {
        Router router = httpServer(vertx);
        router.route().handler(new CorsMiddleware(router, allowedOrigin));
        router.route().handler(ctx -> ctx.response().setStatusCode(200).end("ok"));
    }

    private Router httpServer(Vertx vertx) throws InterruptedException {
        Router router = Router.router(vertx);

        final CountDownLatch latch = new CountDownLatch(1);

        vertx.createHttpServer().requestHandler(router::handle).listen(port, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            latch.countDown();
        });

        latch.await();
        return router;
    }
}
