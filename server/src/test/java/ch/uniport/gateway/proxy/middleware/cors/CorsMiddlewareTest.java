package ch.uniport.gateway.proxy.middleware.cors;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.uniportGateway;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static io.vertx.core.http.HttpHeaders.ORIGIN;

import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class CorsMiddlewareTest extends MiddlewareTestBase {

    private static final String HOST = "localhost";

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CorsMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CorsMiddlewareFactory.ALLOWED_ORIGINS, JsonArray.of("http://example.com"))))));

        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CorsMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CorsMiddlewareFactory.ALLOWED_ORIGINS,
                        JsonArray.of("http://example.com", "https://example.org"),
                        CorsMiddlewareFactory.ALLOWED_ORIGIN_PATTERNS,
                        JsonArray.of("http://(a|b)\\.example.com"),
                        CorsMiddlewareFactory.ALLOWED_HEADERS, JsonArray.of("HEADER-A", "HEADER-B"),
                        CorsMiddlewareFactory.EXPOSED_HEADERS, JsonArray.of("HEADER-A", "HEADER-B"),
                        CorsMiddlewareFactory.MAX_AGE_SECONDS, 42,
                        CorsMiddlewareFactory.ALLOW_CREDENTIALS, false,
                        CorsMiddlewareFactory.ALLOW_PRIVATE_NETWORK, false)))));

        final JsonObject emptyOrigin = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CorsMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CorsMiddlewareFactory.ALLOWED_HEADERS, JsonArray.of(""))))));

        final JsonObject unknownMethod = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CorsMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CorsMiddlewareFactory.ALLOWED_METHODS, JsonArray.of("BLUB"))))));

        final JsonObject illegalMaxAgeType = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CorsMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CorsMiddlewareFactory.MAX_AGE_SECONDS, false)))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CorsMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CorsMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept simple cors", minimal, complete, expectedTrue),
            Arguments.of("accept full cors", simple, complete, expectedTrue),
            Arguments.of("reject cors with empty origin", emptyOrigin, complete, expectedFalse),
            Arguments.of("reject cors with unknown method", unknownMethod, complete, expectedFalse),
            Arguments.of("reject cors with illegal max age type", illegalMaxAgeType, complete, expectedFalse),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    public void testGETNoOrigin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final MiddlewareServer gateway = uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start();
        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (resp) -> {
            // then
            assertThat(testCtx, resp)
                .hasStatusCode(200);
            testCtx.completeNow();
        });
    }

    @Test
    public void testGETVaryOrigin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(List.of("http://example.com", "http://example.org"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(HttpHeaders.VARY.toString(), ORIGIN.toString());
                    testCtx.completeNow();
                });
    }

    @Test
    public void testGETOriginAllowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware("http://example.com")
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "http://example.com");
                    testCtx.completeNow();
                });
    }

    @Test
    public void testGETOriginAllowedPattern(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(List.of(), List.of("http://(a|b).example.com"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://a.example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "http://a.example.com");
                    testCtx.completeNow();
                });
    }

    @Test
    public void testGETAllAllowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware("*")
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "*");
                    testCtx.completeNow();
                });
    }

    @Test
    public void testGETAllAllowedPattern(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(List.of(), List.of(".*"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.GET, "/",
                new RequestOptions().addHeader(ORIGIN, "http://example.com"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(200)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "*");
                    testCtx.completeNow();
                });
    }

    @Test
    public void testGETOriginNotAllowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
    public void testGETOriginNoAllowedPattern(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(List.of(), List.of("http://(a|b).example.com"))
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
    public void testGETNoAllowedOriginsImpliesAllOrigins(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
    public void testOPTIONSNoOrigin(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
    public void testOPTIONSOriginAllowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
    public void testOPTIONSOriginNotAllowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
    public void testOPTIONSOriginWithDifferentSchemeNotAllowed(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
    public void testOPTIONSOriginWithDifferentPortNotAllowed(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
    public void testOPTIONSAllowedMethods(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                Set.of(HttpMethod.GET, HttpMethod.POST),
                Set.of())
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
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS.toString(), Set.of("POST", "GET"));
                    testCtx.completeNow();
                });
    }

    @Test
    public void testOPTIONSAllowedHeaders(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                Set.of(HttpMethod.GET),
                Set.of("X-EXAMPLE-A", "X-EXAMPLE-B"))
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET"), // this is a required header for the
                // preflight request
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS.toString(), Set.of("GET"))
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS.toString(),
                            Set.of("X-EXAMPLE-A", "X-EXAMPLE-B"));
                    testCtx.completeNow();
                });
    }

    @Test
    public void testOPTIONSAllowedHeadersEcho(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                Set.of(HttpMethod.GET),
                Set.of())
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET") // this is a required header for the
                    // preflight request
                    .addHeader(ACCESS_CONTROL_REQUEST_HEADERS, "X-EXAMPLE-A,X-EXAMPLE-B"),
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS.toString(), Set.of("GET"))
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS.toString(),
                            Set.of("X-EXAMPLE-A", "X-EXAMPLE-B"))
                        .hasHeader(HttpHeaders.VARY.toString(), ACCESS_CONTROL_REQUEST_HEADERS.toString());
                    testCtx.completeNow();
                });
    }

    @Test
    public void testGETExposeHeaders(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        uniportGateway(vertx, HOST, testCtx)
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
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS.toString(),
                            Set.of("Content-Encoding", "Uniport-Version"));
                    testCtx.completeNow();
                });
    }

    @Test
    public void testOPTIONSMaxAGE(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final int maxAge = 42;
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                maxAge)
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET"), // this is a required header for the
                // preflight request
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE.toString(), String.valueOf(maxAge));
                    testCtx.completeNow();
                });
    }

    @Test
    public void testOPTIONSAllowCredentials(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final boolean allowCredentials = true;
        uniportGateway(vertx, HOST, testCtx)
            .withCorsMiddleware(
                List.of("http://example.com"),
                allowCredentials)
            .build().start()
            // when
            .incomingRequest(HttpMethod.OPTIONS, "/",
                new RequestOptions()
                    .addHeader(ORIGIN, "http://example.com")
                    .addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET"), // this is a required header for the
                // preflight request
                (resp) -> {
                    // then
                    assertThat(testCtx, resp)
                        .hasStatusCode(204)
                        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(),
                            String.valueOf(allowCredentials));
                    testCtx.completeNow();
                });
    }
}
