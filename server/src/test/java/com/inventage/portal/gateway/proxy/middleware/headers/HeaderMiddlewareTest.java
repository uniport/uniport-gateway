package com.inventage.portal.gateway.proxy.middleware.headers;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddleware;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
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
                        (new HeadersMultiMap()).setAll(Map.ofEntries(entry("X-Custom-Request-Header", "test_request"))),
                        (new HeadersMultiMap()).setAll(
                                Map.ofEntries(entry("Foo", "bar"), entry("X-Custom-Request-Header", "test_request")))),
                Arguments.of("delete a header",
                        (new HeadersMultiMap())
                                .setAll(Map.ofEntries(entry("X-Custom-Request-Header", ""), entry("Foo", ""))),
                        new HeadersMultiMap()),
                Arguments.of("override a header", (new HeadersMultiMap()).setAll(Map.ofEntries(entry("Foo", "test"))),
                        (new HeadersMultiMap()).setAll(Map.ofEntries(entry("Foo", "test")))));
    }

    @ParameterizedTest
    @MethodSource("requestHeaderTestData")
    void requestHeaderTest(String name, MultiMap reqHeaders, MultiMap expectedReqHeaders, Vertx vertx,
            VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();

        String errMsgFormat = "Failure of '%s' test case: %s";

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        HeaderMiddleware header = new HeaderMiddleware(reqHeaders, new HeadersMultiMap());

        Router router = Router.router(vertx);
        router.route().handler(header).handler(ctx -> ctx.response().end("ok"));
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            // cannot use assertEquals since response headers include
            // date, content-length etc.
            assertHeaders(expectedReqHeaders, req.headers(), errMsgFormat, name);
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();

            vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> {
                req.putHeader("Foo", "bar"); // set some request header to test deletion/overwriting
                return req.send();
            }).onComplete(testCtx.succeeding(resp -> {
                responseReceived.flag();
            }));
        }));
    }

    static Stream<Arguments> responseHeaderTestData() {
        return Stream.of(//
                Arguments.of("Test Simple Response",
                        (new HeadersMultiMap())
                                .setAll(Map.ofEntries(entry("Testing", "foo"), entry("Testing2", "bar"))),
                        (new HeadersMultiMap()).setAll(
                                Map.ofEntries(entry("Foo", "bar"), entry("Testing", "foo"), entry("Testing2", "bar")))),
                Arguments.of("empty Custom Header",
                        (new HeadersMultiMap()).setAll(Map.ofEntries(entry("Testing", "foo"), entry("Testing2", ""))),
                        (new HeadersMultiMap()).setAll(Map.ofEntries(entry("Foo", "bar"), entry("Testing", "foo")))),
                Arguments.of("Deleting Custom Header",
                        (new HeadersMultiMap()).setAll(Map.ofEntries(entry("Testing", "foo"), entry("Foo", ""))),
                        (new HeadersMultiMap()).setAll(Map.ofEntries(entry("Testing", "foo")))));
    }

    @ParameterizedTest
    @MethodSource("responseHeaderTestData")
    void responseHeaderTest(String name, MultiMap respHeaders, MultiMap expectedRespHeaders, Vertx vertx,
            VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        int servicePort = TestUtils.findFreePort();

        String errMsgFormat = "Failure of '%s' test case: %s";

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint serviceStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        HeaderMiddleware header = new HeaderMiddleware(new HeadersMultiMap(), respHeaders);

        Router serviceRouter = Router.router(vertx);
        serviceRouter.route().handler(ctx -> {
            ctx.response().headers().add("Foo", "bar"); // set some response header to test deletion/overwriting
            ctx.response().end();
        });
        vertx.createHttpServer().requestHandler(serviceRouter).listen(servicePort).onComplete(testCtx.succeeding(s -> {
            serviceStarted.flag();

            ProxyMiddleware proxy = new ProxyMiddleware(vertx, host, servicePort);

            Router router = Router.router(vertx);
            router.route().handler(header).handler(proxy);
            vertx.createHttpServer().requestHandler(req -> {
                router.handle(req);
                requestServed.flag();
            }).listen(port).onComplete(testCtx.succeeding(p -> {
                serverStarted.flag();

                vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> req.send())
                        .onComplete(testCtx.succeeding(resp -> {
                            // cannot use assertEquals since response headers include
                            // date, content-length etc.
                            assertHeaders(expectedRespHeaders, resp.headers(), errMsgFormat, name);
                            responseReceived.flag();
                        }));
            }));
        }));

    }

    void assertHeaders(MultiMap expected, MultiMap actual, String errMsgFormat, String name) {
        for (Entry<String, String> h : expected.entries()) {
            if (h.getValue().length() == 0) {
                assertFalse(actual.contains(h.getKey()), String.format(errMsgFormat, name,
                        String.format("should not contain request header key '%s'", h.getKey())));
            } else {
                assertTrue(actual.contains(h.getKey()), String.format(errMsgFormat, name,
                        String.format("should contain request header key '%s'", h.getKey())));
                assertTrue(actual.getAll(h.getKey()).contains(h.getValue()), String.format(errMsgFormat, name,
                        String.format("should contain request header '%s:%s'", h.getKey(), h.getValue())));
            }
        }
    }
}
