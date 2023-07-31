package com.inventage.portal.gateway.proxy.middleware.cors;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.vertx.core.http.HttpHeaders.ORIGIN;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class CorsMiddlewareTest {

    private static final String host = "localhost";

    @Test
    public void test_GET_no_origin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final MiddlewareServer portalGateway = portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://portal.minikube").build().start();
        // when
        portalGateway.incomingRequest(HttpMethod.GET, "/", (resp) -> {
            // then
            assertThat(resp).hasStatusCode(200);
            testCtx.completeNow();
        });
    }

    @Test
    public void test_GET_origin_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://portal.minikube").build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://portal.minikube"),
                (resp) -> {
                    // then
                    assertThat(resp)
                        .hasStatusCode(200)
                        .hasHeader(ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "http://portal.minikube");
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_all_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("*").build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://other.com"),
                (resp) -> {
                    // then
                    assertThat(resp)
                        .hasStatusCode(200)
                        .hasHeader(ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "*");
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_origin_not_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://portal.minikube").build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://bad.com"),
                (resp) -> {
                    // then
                    assertThat(resp).hasStatusCode(403);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_no_origin2(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://portal.minikube").build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/", (resp) -> {
                // then
                assertThat(resp).hasStatusCode(200);
                testCtx.completeNow();
            });
    }

}
