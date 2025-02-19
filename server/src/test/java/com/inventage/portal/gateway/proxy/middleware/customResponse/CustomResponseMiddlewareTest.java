package com.inventage.portal.gateway.proxy.middleware.customResponse;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class CustomResponseMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(JsonObject.of(
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT, "test",
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, 200,
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_HEADERS, JsonObject.of(
                            "foo", "bar"))))));

        final JsonObject wrongStatusCodeType = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(JsonObject.of(
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT, "test",
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, "200")))));

        final JsonObject wrongContentType = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(JsonObject.of(
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT, 200,
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, 200)))));

        final JsonObject wrongHeaders = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(JsonObject.of(
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT, "test",
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, 200,
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_HEADERS, JsonObject.of(
                            "X-FOO", 2))))));

        final JsonObject wrongStatusCodeMin = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(JsonObject.of(
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT, "test",
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, 99)))));

        final JsonObject wrongStatusCodeMax = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(JsonObject.of(
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT, "test",
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, 600)))));

        final JsonObject missingRequiredProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(JsonObject.of(
                        CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, 200)))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CustomResponseMiddlewareFactory.CUSTOM_RESPONSE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("custom response middleware", simple, complete, expectedTrue),
            Arguments.of("reject custom response middleware with wrong status code type", wrongStatusCodeType, complete, expectedFalse),
            Arguments.of("reject custom response middleware with wrong status code (min)", wrongStatusCodeMin, complete, expectedFalse),
            Arguments.of("reject custom response middleware with wrong status code (max)", wrongStatusCodeMax, complete, expectedFalse),
            Arguments.of("reject custom response middleware with wrong content type", wrongContentType, complete, expectedFalse),
            Arguments.of("reject custom response middleware with wrong headers", wrongHeaders, complete, expectedFalse),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with missing required property", missingRequiredProperty, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    static Stream<Arguments> contentTestData() {
        return Stream.of(
            Arguments.of("test", "test", 200, HeadersMultiMap.headers().add("X-Foo", "bar").add("X-Bar", "baz")),
            Arguments.of("test", "test", 400, HeadersMultiMap.headers()),
            Arguments.of("", "", 204, HeadersMultiMap.headers().add("X-Bar", "baz")),
            Arguments.of(null, "", 204, HeadersMultiMap.headers()));
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
