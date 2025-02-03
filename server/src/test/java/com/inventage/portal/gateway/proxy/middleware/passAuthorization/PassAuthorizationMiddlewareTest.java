package com.inventage.portal.gateway.proxy.middleware.passAuthorization;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization.PassAuthorizationMiddlewareFactory;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class PassAuthorizationMiddlewareTest extends MiddlewareTestBase {

    private static final String host = "localhost";

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", PassAuthorizationMiddlewareFactory.PASS_AUTHORIZATION,
                    withMiddlewareOpts(JsonObject.of(
                        WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS, JsonArray.of(
                            JsonObject.of(
                                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY, "Ymx1Ygo=",
                                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM, "RS256")),
                        WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ISSUER, "bar",
                        WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_AUDIENCE, JsonArray.of("blub"),
                        PassAuthorizationMiddlewareFactory.PASS_AUTHORIZATION_SESSION_SCOPE, "blub")))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", PassAuthorizationMiddlewareFactory.PASS_AUTHORIZATION)));

        return Stream.of(
            Arguments.of("valid config", simple, complete, expectedTrue),
            Arguments.of("invalid config with missing options", missingOptions, complete, expectedFalse));
    }

    @Test
    public void testNoBearer(Vertx vertx, VertxTestContext testCtx) {

        portalGateway(vertx, host, testCtx)
            .withSessionMiddleware()
            .withPassAuthorizationMiddleware("testScope", new MockJWTAuth(new JsonObject(), "someToken"))
            .build()
            .start()
            .incomingRequest(GET, "/", (resp) -> {
                // then
                VertxAssertions.assertEquals(testCtx, 401, resp.statusCode(), "unexpected status code");
                testCtx.completeNow();
            });

    }

    @Test
    public void testWithBearerNotAuthorized(Vertx vertx, VertxTestContext testCtx) {

        portalGateway(vertx, host, testCtx)
            .withSessionMiddleware()
            .withMockOAuth2Middleware("unauthorizedAuthHeader")
            .withPassAuthorizationMiddleware("testScope", new MockJWTAuth(new JsonObject(), "authorizedAuthHeader"))
            .build()
            .start()
            .incomingRequest(GET, "/", (resp) -> {
                // then
                VertxAssertions.assertEquals(testCtx, 401, resp.statusCode(), "unexpected status code");
                testCtx.completeNow();
            });

    }

    @Test
    public void testWithBearerAuthorized(Vertx vertx, VertxTestContext testCtx) {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final String externalAuthHeader = "externalAuthHeader";
        final String internalAuthHeader = "internalAuthHeader";

        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.AUTHORIZATION, externalAuthHeader);

        // when
        portalGateway(vertx, host, testCtx)
            .withSessionMiddleware()
            .withMockOAuth2Middleware(internalAuthHeader)
            .withPassAuthorizationMiddleware("testScope", new MockJWTAuth(new JsonObject(), internalAuthHeader))
            .withRoutingContextHolder(routingContext)
            .build()
            .start()
            .incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (resp) -> {
                // then
                VertxAssertions.assertEquals(testCtx, 200, resp.statusCode(), "unexpected status code");
                VertxAssertions.assertTrue(testCtx, routingContext.get().request().headers().contains(HttpHeaders.AUTHORIZATION));
                VertxAssertions.assertEquals(testCtx, routingContext.get().request().getHeader(HttpHeaders.AUTHORIZATION), externalAuthHeader);
                testCtx.completeNow();
            });

    }
}
