package com.inventage.portal.gateway.proxy.middleware.headers;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import com.inventage.portal.gateway.TestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class HeaderMiddlewareTest {

    String host = "localhost";

    static Stream<Arguments> requestHeaderTestData() {
        return Stream.of(//
                Arguments.of("adds a header",
                        Map.ofEntries(entry("X-Custom-Request-Header", "test_request")),
                        Map.ofEntries(entry("Foo", "bar"),
                                entry("X-Custom-Request-Header", "test_request"))),
                Arguments.of("delete a header",
                        Map.ofEntries(entry("X-Custom-Request-Header", ""), entry("Foo", "")),
                        Map.ofEntries()),
                Arguments.of("override a header", Map.ofEntries(entry("Foo", "test")),
                        Map.ofEntries(entry("Foo", "test"))));
    }

    @ParameterizedTest
    @MethodSource("requestHeaderTestData")
    void requestHeaderTest(String name, Map<String, String> reqHeaders,
            Map<String, String> expectedReqHeaders, Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();

        String failureMsgFormat = "Failure of '%s' test case: %s";

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        HeaderMiddleware header = new HeaderMiddleware(reqHeaders, Map.ofEntries());

        Router router = Router.router(vertx);
        router.route().handler(header).handler(ctx -> ctx.response().end("ok"));
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                for (Entry<String, String> h : expectedReqHeaders.entrySet()) {
                    if (h.getValue().length() == 0) {
                        assertFalse(req.headers().contains(h.getKey()),
                                String.format(failureMsgFormat, name, String.format(
                                        "should not contain request header key '%s'", h.getKey())));
                    } else {
                        assertTrue(req.headers().contains(h.getKey()),
                                String.format(failureMsgFormat, name, String.format(
                                        "should contain request header key '%s'", h.getKey())));
                        assertTrue(req.headers().getAll(h.getKey()).contains(h.getValue()),
                                String.format(failureMsgFormat, name,
                                        String.format("should contain request header '%s:%s'",
                                                h.getKey(), h.getValue())));
                    }
                }
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(httpServer -> serverStarted.flag()));

        vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> {
            req.putHeader("Foo", "bar"); // set some request header to test deletion/overwriting
            return req.send();
        }).onComplete(testCtx.succeeding(resp -> {
            responseReceived.flag();
        }));
    }

    static Stream<Arguments> responseHeaderTestData() {
        return Stream.of(//
                Arguments.of("Test Simple Response",
                        Map.ofEntries(entry("Testing", "foo"), entry("Testing2", "bar")),
                        Map.ofEntries(entry("Foo", "bar"), entry("Testing", "foo"),
                                entry("Testing2", "bar"))),
                Arguments.of("empty Custom Header",
                        Map.ofEntries(entry("Testing", "foo"), entry("Testing2", "")),
                        Map.ofEntries(entry("Foo", "bar"), entry("Testing", "foo"))),
                Arguments.of("Deleting Custom Header",
                        Map.ofEntries(entry("Testing", "foo"), entry("Foo", "")),
                        Map.ofEntries(entry("Testing", "foo"))));
    }

    @ParameterizedTest
    @MethodSource("responseHeaderTestData")
    void responseHeaderTest(String name, Map<String, String> respHeaders,
            Map<String, String> expectedRespHeaders, Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();

        String failureMsgFormat = "Failure of '%s' test case: %s";

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        HeaderMiddleware header = new HeaderMiddleware(Map.ofEntries(), respHeaders);

        Router router = Router.router(vertx);
        router.route().handler(header).handler(ctx -> ctx.response().end("ok"));
        vertx.createHttpServer().requestHandler(req -> {
            req.response().headers().add("Foo", "bar"); // set some response header to test deletion/overwriting
            router.handle(req);
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(httpServer -> serverStarted.flag()));

        vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub")
                .compose(req -> req.send()).onComplete(testCtx.succeeding(resp -> {
                    testCtx.verify(() -> {
                        for (Entry<String, String> h : expectedRespHeaders.entrySet()) {
                            if (h.getValue().length() == 0) {
                                assertFalse(resp.headers().contains(h.getKey()),
                                        String.format(failureMsgFormat, name, String.format(
                                                "should not contain response header key '%s'",
                                                h.getKey())));
                            } else {
                                assertTrue(resp.headers().contains(h.getKey()),
                                        String.format(failureMsgFormat, name,
                                                String.format(
                                                        "should contain response header key '%s'",
                                                        h.getKey())));
                                assertTrue(resp.headers().getAll(h.getKey()).contains(h.getValue()),
                                        String.format(failureMsgFormat, name,
                                                String.format(
                                                        "should contain response header '%s:%s'",
                                                        h.getKey(), h.getValue())));
                            }
                        }
                    });
                    responseReceived.flag();
                }));
    }
}
