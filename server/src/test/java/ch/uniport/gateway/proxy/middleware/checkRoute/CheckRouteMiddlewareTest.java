package ch.uniport.gateway.proxy.middleware.checkRoute;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;

import ch.uniport.gateway.proxy.middleware.BrowserConnected;
import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(VertxExtension.class)
public class CheckRouteMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CheckRouteMiddlewareFactory.TYPE)));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CheckRouteMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CheckRouteMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept minimal config", minimal, complete, expectedTrue),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/" + CheckRouteMiddlewareFactory.PATH,
        "/a/b/" + CheckRouteMiddlewareFactory.PATH,
        "/a/b/" + CheckRouteMiddlewareFactory.PATH + "/c/d",
        "/prefix" + CheckRouteMiddlewareFactory.PATH + "postfix"
    })
    public void isHandledByCheckRoute(String uri, Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withAuthenticationTriggerMiddleware()
            .withMiddleware(ctx -> {
                ctx.response().setStatusCode(200).end();
            })
            .build().start();
        BrowserConnected browser = gateway.connectBrowser();
        // when
        browser.request(GET, uri).whenComplete((response, error) -> {
            // then
            assertThat(testCtx, response)
                .hasStatusCode(202);
            testCtx.completeNow();
        });
    }

    @Test
    public void otherPathIsIgnored(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withAuthenticationTriggerMiddleware()
            .withMiddleware(ctx -> {
                ctx.response().setStatusCode(200).end();
            })
            .build().start();
        BrowserConnected browser = gateway.connectBrowser();
        // when
        browser.request(GET, "/not-a-check-route-path").whenComplete((response, error) -> {
            // then
            assertThat(testCtx, response)
                .hasStatusCode(200);
            testCtx.completeNow();
        });
    }
}
