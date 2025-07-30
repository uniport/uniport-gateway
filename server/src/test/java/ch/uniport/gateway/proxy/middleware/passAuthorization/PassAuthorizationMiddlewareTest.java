package ch.uniport.gateway.proxy.middleware.passAuthorization;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.uniportGateway;
import static io.vertx.core.http.HttpMethod.GET;

import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import ch.uniport.gateway.proxy.middleware.VertxAssertions;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import ch.uniport.gateway.proxy.middleware.authorization.passAuthorization.PassAuthorizationMiddlewareFactory;
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

    private static final String HOST = "localhost";

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", PassAuthorizationMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEYS, JsonArray.of(
                            JsonObject.of(
                                WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY, "Ymx1Ygo=",
                                WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, "RS256")),
                        WithAuthHandlerMiddlewareFactoryBase.ISSUER, "bar",
                        WithAuthHandlerMiddlewareFactoryBase.AUDIENCE, JsonArray.of("blub"),
                        PassAuthorizationMiddlewareFactory.SESSION_SCOPE, "blub")))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", PassAuthorizationMiddlewareFactory.TYPE)));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse));
    }

    @Test
    public void testNoBearer(Vertx vertx, VertxTestContext testCtx) {

        uniportGateway(vertx, HOST, testCtx)
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

        uniportGateway(vertx, HOST, testCtx)
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
        uniportGateway(vertx, HOST, testCtx)
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
