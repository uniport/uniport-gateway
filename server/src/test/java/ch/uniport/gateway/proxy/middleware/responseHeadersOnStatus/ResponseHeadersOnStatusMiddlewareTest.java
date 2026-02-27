package ch.uniport.gateway.proxy.middleware.responseHeadersOnStatus;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.uniportGateway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class ResponseHeadersOnStatusMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject validWithSet = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 401,
                        ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS, JsonObject.of(
                            "WWW-Authenticate", "Bearer realm=\"test\""))))));

        final JsonObject validWithRewrite = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 401,
                        ResponseHeadersOnStatusMiddlewareFactory.REWRITE_RESPONSE_HEADERS, JsonObject.of(
                            "WWW-Authenticate", JsonObject.of(
                                ResponseHeadersOnStatusMiddlewareFactory.REGEX, "(resource_metadata=\"[^\"]*\")",
                                ResponseHeadersOnStatusMiddlewareFactory.REPLACEMENT, "$1/suffix")))))));

        final JsonObject validWithBoth = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 401,
                        ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS, JsonObject.of(
                            "X-Custom", "value"),
                        ResponseHeadersOnStatusMiddlewareFactory.REWRITE_RESPONSE_HEADERS, JsonObject.of(
                            "WWW-Authenticate", JsonObject.of(
                                ResponseHeadersOnStatusMiddlewareFactory.REGEX, "old",
                                ResponseHeadersOnStatusMiddlewareFactory.REPLACEMENT, "new")))))));

        final JsonObject missingBothSetAndRewrite = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 401)))));

        final JsonObject invalidRegex = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 401,
                        ResponseHeadersOnStatusMiddlewareFactory.REWRITE_RESPONSE_HEADERS, JsonObject.of(
                            "WWW-Authenticate", JsonObject.of(
                                ResponseHeadersOnStatusMiddlewareFactory.REGEX, "[invalid(",
                                ResponseHeadersOnStatusMiddlewareFactory.REPLACEMENT, "test")))))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 401,
                        "unknownProp", "value")))));

        final JsonObject wrongStatusCodeType = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, "401",
                        ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS, JsonObject.of(
                            "X-Custom", "value"))))));

        final JsonObject statusCodeTooLow = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 99,
                        ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS, JsonObject.of(
                            "X-Custom", "value"))))));

        final JsonObject statusCodeTooHigh = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseHeadersOnStatusMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, 600,
                        ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS, JsonObject.of(
                            "X-Custom", "value"))))));

        return Stream.of(
            Arguments.of("valid config with setResponseHeaders", validWithSet, complete, expectedTrue),
            Arguments.of("valid config with rewriteResponseHeaders", validWithRewrite, complete, expectedTrue),
            Arguments.of("valid config with both set and rewrite", validWithBoth, complete, expectedTrue),
            Arguments.of("reject config missing both set and rewrite", missingBothSetAndRewrite, complete, expectedFalse),
            Arguments.of("reject config with invalid regex", invalidRegex, complete, expectedFalse),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse),
            Arguments.of("reject config with wrong statusCode type", wrongStatusCodeType, complete, expectedFalse),
            Arguments.of("reject config with statusCode too low", statusCodeTooLow, complete, expectedFalse),
            Arguments.of("reject config with statusCode too high", statusCodeTooHigh, complete, expectedFalse));
    }

    @Test
    void setMode_matchingStatusCode_replacesHeader(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withResponseHeadersOnStatusMiddleware(
                401,
                HeadersMultiMap.httpHeaders().add("WWW-Authenticate", "Bearer realm=\"replaced\""),
                null)
            .build(ctx -> ctx.response().setStatusCode(401)
                .putHeader("WWW-Authenticate", "Bearer realm=\"original\"")
                .end("unauthorized"))
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertEquals(401, response.statusCode());
            assertEquals("Bearer realm=\"replaced\"", response.getHeader("WWW-Authenticate"));
            testCtx.completeNow();
        });
    }

    @Test
    void setMode_nonMatchingStatusCode_doesNotModifyHeaders(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withResponseHeadersOnStatusMiddleware(
                401,
                HeadersMultiMap.httpHeaders().add("WWW-Authenticate", "Bearer realm=\"replaced\""),
                null)
            .build(ctx -> ctx.response().setStatusCode(200)
                .putHeader("WWW-Authenticate", "Bearer realm=\"original\"")
                .end("ok"))
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertEquals(200, response.statusCode());
            assertEquals("Bearer realm=\"original\"", response.getHeader("WWW-Authenticate"));
            testCtx.completeNow();
        });
    }

    @Test
    void setMode_differentStatusCode_doesNotModifyHeaders(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withResponseHeadersOnStatusMiddleware(
                401,
                HeadersMultiMap.httpHeaders().add("WWW-Authenticate", "Bearer realm=\"replaced\""),
                null)
            .build(ctx -> ctx.response().setStatusCode(403).end("forbidden"))
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertEquals(403, response.statusCode());
            assertNull(response.getHeader("WWW-Authenticate"));
            testCtx.completeNow();
        });
    }

    @Test
    void rewriteMode_matchingStatusCode_rewritesHeader(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String originalHeader = "Bearer resource_metadata=\"http://localhost:20001/.well-known/oauth-protected-resource\", scope=\"Organisation\"";
        final String expectedHeader = "Bearer resource_metadata=\"http://localhost:20001/.well-known/oauth-protected-resource/organisation/mcp\", scope=\"Organisation\"";

        final Map<String, ResponseHeadersOnStatusMiddleware.CompiledRewriteRule> rewriteRules = Map.of(
            "WWW-Authenticate", new ResponseHeadersOnStatusMiddleware.CompiledRewriteRule(
                Pattern.compile("(resource_metadata=\"[^\"]*\\.well-known/oauth-protected-resource)(\")"),
                "$1/organisation/mcp$2"));

        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withResponseHeadersOnStatusMiddleware(401, null, rewriteRules)
            .build(ctx -> ctx.response().setStatusCode(401)
                .putHeader("WWW-Authenticate", originalHeader)
                .end("unauthorized"))
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertEquals(401, response.statusCode());
            assertEquals(expectedHeader, response.getHeader("WWW-Authenticate"));
            testCtx.completeNow();
        });
    }

    @Test
    void rewriteMode_headerNotPresent_doesNotAddHeader(Vertx vertx, VertxTestContext testCtx) {
        // given
        final Map<String, ResponseHeadersOnStatusMiddleware.CompiledRewriteRule> rewriteRules = Map.of(
            "WWW-Authenticate", new ResponseHeadersOnStatusMiddleware.CompiledRewriteRule(
                Pattern.compile("old"),
                "new"));

        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withResponseHeadersOnStatusMiddleware(401, null, rewriteRules)
            .build(ctx -> ctx.response().setStatusCode(401).end("unauthorized"))
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertEquals(401, response.statusCode());
            assertNull(response.getHeader("WWW-Authenticate"));
            testCtx.completeNow();
        });
    }

    @Test
    void rewriteMode_nonMatchingStatusCode_doesNotModifyHeaders(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String originalHeader = "Bearer realm=\"original\"";

        final Map<String, ResponseHeadersOnStatusMiddleware.CompiledRewriteRule> rewriteRules = Map.of(
            "WWW-Authenticate", new ResponseHeadersOnStatusMiddleware.CompiledRewriteRule(
                Pattern.compile("original"),
                "rewritten"));

        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withResponseHeadersOnStatusMiddleware(401, null, rewriteRules)
            .build(ctx -> ctx.response().setStatusCode(200)
                .putHeader("WWW-Authenticate", originalHeader)
                .end("ok"))
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertEquals(200, response.statusCode());
            assertEquals(originalHeader, response.getHeader("WWW-Authenticate"));
            testCtx.completeNow();
        });
    }
}
