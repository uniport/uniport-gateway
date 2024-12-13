package com.inventage.portal.gateway.proxy.middleware.cors;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_MAX_AGE;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static io.vertx.core.http.HttpHeaders.ORIGIN;
import static io.vertx.core.http.HttpHeaders.VARY;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class CorsMiddlewareTest {

    private static final String host = "localhost";

    @Test
    public void test_GET_no_origin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final MiddlewareServer portalGateway = portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start();
        // when
        portalGateway.incomingRequest(HttpMethod.GET, "/", (resp) -> {
            // then
            assertThat(testCtx, resp)
                .hasStatusCode(200);
            testCtx.completeNow();
        });
    }

    @Test
    public void test_GET_vary_origin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(List.of("http://example.com", "http://example.org"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(VARY.toString(), ORIGIN.toString());
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_origin_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "http://example.com");
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_origin_allowed_pattern(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(null, List.of("http://(a|b).example.com"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://a.example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "http://a.example.com");
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_all_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("*")
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "*");
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_all_allowed_pattern(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(null, List.of(".*"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "*");
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_origin_not_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://bad.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(403);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_origin_not_allowed_pattern(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(null, List.of("http://(a|b).example.com"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://bad.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(403);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_no_allowed_origins_implies_all_origins(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(List.of(), List.of())
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_no_origin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/", (resp) -> {
                // then
                assertThat(testCtx, resp)
                    .hasStatusCode(200);
                testCtx.completeNow();
            });
    }

    @Test
    public void test_OPTIONS_origin_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_origin_not_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions().addHeader(ORIGIN, "http://bad.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(403);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_origin_with_different_scheme_not_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions().addHeader(ORIGIN, "https://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(403);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_origin_with_different_port_not_allowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com:1234"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(403);
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_allowed_methods(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                Set.of(HttpMethod.GET, HttpMethod.POST),
                null)
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(ACCESS_CONTROL_ALLOW_METHODS.toString(), Set.of("POST", "GET"));
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_allowed_headers(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                Set.of(HttpMethod.GET),
                Set.of("X-EXAMPLE-A", "X-EXAMPLE-B"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET"), // this is a required header for the preflight request 
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(ACCESS_CONTROL_ALLOW_METHODS.toString(), Set.of("GET"))
                        .hasHeader(ACCESS_CONTROL_ALLOW_HEADERS.toString(), Set.of("X-EXAMPLE-A", "X-EXAMPLE-B"));
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_allowed_headers_echo(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                Set.of(HttpMethod.GET),
                null)
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET") // this is a required header for the preflight request 
                    .addHeader(ACCESS_CONTROL_REQUEST_HEADERS, "X-EXAMPLE-A,X-EXAMPLE-B"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(ACCESS_CONTROL_ALLOW_METHODS.toString(), Set.of("GET"))
                        .hasHeader(ACCESS_CONTROL_ALLOW_HEADERS.toString(), Set.of("X-EXAMPLE-A", "X-EXAMPLE-B"))
                        .hasHeader(VARY.toString(), ACCESS_CONTROL_REQUEST_HEADERS.toString());
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_GET_expose_headers(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                Set.of("Content-Encoding", "Uniport-Version"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(ACCESS_CONTROL_EXPOSE_HEADERS.toString(), Set.of("Content-Encoding", "Uniport-Version"));
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_max_age(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final int maxAge = 42;
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                maxAge)
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET"), // this is a required header for the preflight request 
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(ACCESS_CONTROL_MAX_AGE.toString(), String.valueOf(maxAge));
                    testCtx.completeNow();
                });
    }

    @Test
    public void test_OPTIONS_allow_credentials(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final boolean allowCredentials = true;
        portalGateway(vertx, host, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                allowCredentials)
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET"), // this is a required header for the preflight request 
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(), String.valueOf(allowCredentials));
                    testCtx.completeNow();
                });
    }
}
