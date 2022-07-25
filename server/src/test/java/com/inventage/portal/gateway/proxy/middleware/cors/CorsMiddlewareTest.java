package com.inventage.portal.gateway.proxy.middleware.cors;

import com.inventage.portal.gateway.TestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class CorsMiddlewareTest {

    private static final String host = "localhost";
    private int port;

    @BeforeEach
    public void setup() {
        port = TestUtils.findFreePort();
    }

    @Test
    public void test_GET_no_origin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, port).withCorsMiddleware("http://portal.minikube")
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions(), (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    assertEquals("origin", resp.getHeader(VARY));
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_origin_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, port).withCorsMiddleware("http://portal.minikube")
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions().addHeader(ORIGIN, "http://portal.minikube"), (resp) -> {
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
        portalGateway(vertx, host, port).withCorsMiddleware("*")
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions().addHeader(ORIGIN, "http://other.com"), (resp) -> {
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
        portalGateway(vertx, host, port).withCorsMiddleware("http://portal.minikube")
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions().addHeader(ORIGIN, "http://bad.com"), (resp) -> {
                    // then
                    assertEquals(403, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_no_origin2(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, port).withCorsMiddleware("http://portal.minikube")
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions(), (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    assertEquals("origin", resp.getHeader(VARY));
                    testCtx.completeNow();
                });
    }

}
