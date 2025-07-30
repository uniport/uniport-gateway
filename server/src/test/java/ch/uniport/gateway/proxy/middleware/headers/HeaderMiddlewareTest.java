package ch.uniport.gateway.proxy.middleware.headers;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.uniportGateway;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertFalse;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertTrue;

import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class HeaderMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", HeaderMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of(HeaderMiddlewareFactory.HEADERS_REQUEST, JsonObject.of("foo", "bar"))))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", HeaderMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", HeaderMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("reject config with missing options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    public void shouldHaveCustomHeadersinRedirect(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withHeaderMiddleware(
                MultiMap.caseInsensitiveMultiMap(),
                MultiMap.caseInsensitiveMultiMap().add("foo", "bar"))
            .withCustomResponseMiddleware(null, 302, MultiMap.caseInsensitiveMultiMap().add("Location", "/baz"))
            .build().start();
        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (resp) -> {
            // then
            assertThat(testCtx, resp)
                .hasStatusCode(302)
                .hasHeader("Location", "/baz")
                .hasHeader("foo", "bar");
            testCtx.completeNow();
        });
    }

    static Stream<Arguments> headerTestData() {
        return Stream.of(//
            Arguments.of("adds a header",
                HeadersMultiMap.httpHeaders().add("Foo", "bar"),
                HeadersMultiMap.httpHeaders().add("Testing", "test_request"),
                HeadersMultiMap.httpHeaders().add("Foo", "bar").add("Testing", "test_request")),
            Arguments.of("adding multiple headers",
                HeadersMultiMap.httpHeaders().add("Foo", "bar"),
                HeadersMultiMap.httpHeaders().add("Testing", "foo").add("Testing2", "bar"),
                HeadersMultiMap.httpHeaders().add("Foo", "bar").add("Testing", "foo").add("Testing2", "bar")),
            Arguments.of("delete a header",
                HeadersMultiMap.httpHeaders().add("Foo", "bar"),
                HeadersMultiMap.httpHeaders().add("Testing", "foo").add("Foo", ""),
                HeadersMultiMap.httpHeaders().add("Testing", "foo")),
            Arguments.of("do not duplicate a header",
                HeadersMultiMap.httpHeaders().add("Foo", "bar"),
                HeadersMultiMap.httpHeaders().add("Foo", "bar"),
                HeadersMultiMap.httpHeaders().add("Foo", "bar")),
            Arguments.of("adds a header value",
                HeadersMultiMap.httpHeaders().add("Foo", "bar"),
                HeadersMultiMap.httpHeaders().add("Foo", "test"),
                HeadersMultiMap.httpHeaders().add("Foo", "bar").add("Foo", "test")),
            Arguments.of("delete a header",
                HeadersMultiMap.httpHeaders().add("Foo", "bar"),
                HeadersMultiMap.httpHeaders().add("Testing", "foo").add("Foo", ""),
                HeadersMultiMap.httpHeaders().add("Testing", "foo")));
    }

    @ParameterizedTest
    @MethodSource("headerTestData")
    void requestHeaderTest(
        String name, MultiMap reqHeaders, MultiMap reqHeaderModifiers, MultiMap expectedReqHeaders,
        Vertx vertx, VertxTestContext testCtx
    ) {
        // given
        final String errMsgFormat = "Failure of '%s' test case: %s";
        final Handler<RoutingContext> checkHeaders = ctx -> {
            // then
            assertHeaders(testCtx, expectedReqHeaders, ctx.request().headers(), errMsgFormat, name);
            ctx.response().setStatusCode(200).end("ok");
        };
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withHeaderMiddleware(reqHeaderModifiers, new HeadersMultiMap())
            .build(checkHeaders)
            .start();
        final RequestOptions requestOptions = new RequestOptions().setHeaders(reqHeaders);

        // when
        gateway.incomingRequest(HttpMethod.GET, "/",
            requestOptions,
            response -> {
                testCtx.completeNow();
            });
    }

    @ParameterizedTest
    @MethodSource("headerTestData")
    void responseHeaderTest(
        String name, MultiMap respHeaders, MultiMap respHeaderModifiers, MultiMap expectedRespHeaders,
        Vertx vertx, VertxTestContext testCtx
    ) {
        // given
        final String errMsgFormat = "Failure of '%s' test case: %s";
        final Handler<RoutingContext> setMockHeaders = ctx -> {
            ctx.response().headers().addAll(respHeaders);
            ctx.response().setStatusCode(200).end("ok");
        };
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withHeaderMiddleware(new HeadersMultiMap(), respHeaderModifiers)
            .build(setMockHeaders)
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertHeaders(testCtx, expectedRespHeaders, response.headers(), errMsgFormat, name);
            testCtx.completeNow();
        });
    }

    void assertHeaders(VertxTestContext testCtx, MultiMap expected, MultiMap actual, String errMsgFormat, String name) {
        for (String key : expected.names()) {
            if (expected.getAll(key).size() == 0) {
                assertFalse(testCtx, actual.contains(key), String.format(errMsgFormat, name,
                    String.format("should not contain request header key '%s'", key)));
            } else {
                assertTrue(testCtx, actual.contains(key), String.format(errMsgFormat, name,
                    String.format("should contain request header key '%s'", key)));
                assertTrue(testCtx, actual.getAll(key).equals(expected.getAll(key)), String.format(errMsgFormat, name,
                    String.format("should contain request headers '%s': actual '%s', expected '%s'", key, actual.getAll(key), expected.getAll(key))));
            }
        }
    }
}
