package com.inventage.portal.gateway.proxy.middleware.customResponse;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class CustomResponseMiddlewareTest {

    static Stream<Arguments> contentTestData() {
        return Stream.of(
            Arguments.of("test", "test", 200, (new HeadersMultiMap()).setAll(Map.ofEntries(entry("X-Foo", "bar"), entry("X-Bar", "baz")))),
            Arguments.of("test", "test", 400, null),
            Arguments.of("", "", 204, (new HeadersMultiMap()).setAll(Map.ofEntries(entry("X-Bar", "baz")))),
            Arguments.of(null, "", 204, null));
    }

    @ParameterizedTest
    @MethodSource("contentTestData")
    void responseTest(String content, String expectedContent, Integer statusCode, HeadersMultiMap headers, Vertx vertx, VertxTestContext testCtx) {
        // given
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCustomResponseMiddleware(content, statusCode, headers)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (response -> {
            // then
            assertEquals(statusCode, response.statusCode());
            assertHeaders(headers, response.headers());

            response.body().onComplete(testCtx.succeeding(buffer -> {
                assertEquals(expectedContent, buffer.toString());
                testCtx.completeNow();
            }));
        }));
    }

    void assertHeaders(MultiMap expected, MultiMap actual) {
        if (expected == null) {
            assertTrue(true);
            return;
        }

        for (Map.Entry<String, String> header : expected.entries()) {
            assertTrue(actual.contains(header.getKey()), String.format("should contain response header key '%s'", header.getKey()));
            assertTrue(actual.getAll(header.getKey()).contains(header.getValue()), String.format("should contain response header '%s:%s'", header.getKey(), header.getValue()));
        }
    }
}
