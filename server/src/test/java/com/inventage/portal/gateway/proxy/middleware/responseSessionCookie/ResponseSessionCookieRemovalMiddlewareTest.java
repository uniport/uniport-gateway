package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class ResponseSessionCookieRemovalMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseSessionCookieRemovalMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of(ResponseSessionCookieRemovalMiddlewareFactory.SESSION_COOKIE_NAME, "blub")))));

        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseSessionCookieRemovalMiddlewareFactory.TYPE)));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseSessionCookieRemovalMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ResponseSessionCookieRemovalMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("valid config", simple, complete, expectedTrue),
            Arguments.of("minimal config", minimal, complete, expectedTrue),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    public void shouldRemoveSessionCookieInResponse(Vertx vertx, VertxTestContext testCtx) {
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withMiddleware(addingSignal())
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(200)
                .hasNotSetSessionCookie();
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldContainSessionCookieInResponse(Vertx vertx, VertxTestContext testCtx) {
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(200)
                .hasSetSessionCookie(null);
            testCtx.completeNow();
        });
    }

    private Handler<RoutingContext> addingSignal() {
        return ctx -> {
            ResponseSessionCookieRemovalMiddleware.addSignal(ctx);
            ctx.response().end();
        };
    }
}
