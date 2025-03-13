package com.inventage.portal.gateway.proxy.middleware.controlapi;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware.CONTROL_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware.SESSION_BAG_COOKIES;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class ControlApiMiddlewareTest extends MiddlewareTestBase {

    private static final String HOST = "localhost";

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject sessionTermination = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ControlApiMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of(ControlApiMiddlewareFactory.ACTION, ControlApiMiddlewareFactory.ACTION_SESSION_TERMINATE)))));

        final JsonObject sessionReset = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ControlApiMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of(ControlApiMiddlewareFactory.ACTION, ControlApiMiddlewareFactory.ACTION_SESSION_RESET)))));

        final JsonObject invalidAction = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ControlApiMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of(ControlApiMiddlewareFactory.ACTION, "blub")))));

        final JsonObject missingRequiredProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ControlApiMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of()))));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ControlApiMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept config with 'SESSION_TERMINATE' action", sessionTermination, complete, expectedTrue),
            Arguments.of("accept config with 'SESSION_RESET' action", sessionReset, complete, expectedTrue),
            Arguments.of("reject config with invalid action", invalidAction, complete, expectedFalse),
            Arguments.of("reject config with missing required property", missingRequiredProperty, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    void sessionTerminationTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final Cookie sessionTerminateCookie = Cookie.cookie(CONTROL_COOKIE_NAME, ControlApiAction.SESSION_TERMINATE.toString()).setMaxAge(3600);

        final Checkpoint sessionTerminated = testCtx.checkpoint();
        final Checkpoint sessionTerminationServerStarted = testCtx.checkpoint();
        final Checkpoint responseReceived = testCtx.checkpoint();
        final AtomicReference<RoutingContext> routingContextHolder = new AtomicReference<>();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> cookieInsertionHandler = getCookieInsertionHandler(List.of(sessionTerminateCookie));

        final int sessionTerminationPort = TestUtils.findFreePort();
        vertx.createHttpServer()
            .requestHandler(req -> {
                sessionTerminated.flag();
                req.response().setStatusCode(200).end();
            })
            .listen(sessionTerminationPort, testCtx.succeeding(v -> sessionTerminationServerStarted.flag()));

        final WebClient webclient = WebClient.create(vertx, new WebClientOptions()
            .setDefaultPort(sessionTerminationPort)
            .setDefaultHost(HOST));

        portalGateway(vertx, HOST, testCtx)
            .withRoutingContextHolder(routingContextHolder)
            .withSessionMiddleware()
            .withSessionBagMiddleware(List.of())
            .withMockOAuth2Middleware(HOST, sessionTerminationPort)
            .withControlApiMiddleware(ControlApiAction.SESSION_TERMINATE, webclient)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cookieInsertionHandler)
            .build().start()
            // when
            .incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
                // then
                assertSessionTermination(testCtx, outgoingResponse, routingContextHolder.get());
                assertControlCookieRemoval(testCtx, outgoingResponse);
                responseReceived.flag();
            });
    }

    private void assertSessionTermination(VertxTestContext testCtx, HttpClientResponse outgoingResponse, RoutingContext routingContext) {
        VertxAssertions.assertTrue(testCtx, routingContext.session().isDestroyed(), "session should be destroyed");
        VertxAssertions.assertTrue(testCtx, routingContext.session().data().isEmpty(), "all data should be deleted from session");

        VertxAssertions.assertEquals(testCtx, 200, outgoingResponse.statusCode(), "status code should be 200");
        VertxAssertions.assertFalse(testCtx,
            outgoingResponse.headers().getAll(HttpHeaders.SET_COOKIE.toString()).stream()
                .map(cookie -> ClientCookieDecoder.STRICT.decode(cookie))
                .anyMatch(cookie -> cookie.name().equals(CONTROL_COOKIE_NAME)),
            "control cookie should not appear in the response)");
    }

    @Test
    void sessionResetTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final Cookie keycloakIdentityCookie = Cookie.cookie("KEYCLOAK_IDENTITY", "thisisme").setMaxAge(3600);
        final Cookie keycloakTestCookie = Cookie.cookie("KEYCLOAK_TEST", "keycloak").setMaxAge(3600);
        final Cookie sessionResetCookie = Cookie.cookie(CONTROL_COOKIE_NAME, ControlApiAction.SESSION_RESET.toString()).setMaxAge(3600);

        final Checkpoint sessionResetted = testCtx.checkpoint();
        final Checkpoint sessionResetServerStarted = testCtx.checkpoint();
        final Checkpoint responseReceived = testCtx.checkpoint();
        final AtomicReference<RoutingContext> routingContextHolder = new AtomicReference<>();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> cookieInsertionHandler = getCookieInsertionHandler(List.of(keycloakTestCookie, sessionResetCookie));

        final int sessionResetPort = TestUtils.findFreePort();
        vertx.createHttpServer()
            .requestHandler(req -> {
                VertxAssertions.assertTrue(
                    testCtx,
                    req.headers().getAll(HttpHeaders.COOKIE).stream()
                        .map(cookie -> ClientCookieDecoder.STRICT.decode(cookie))
                        .anyMatch(cookie -> cookie.name().equals(keycloakIdentityCookie.getName()) && cookie.value().equals(keycloakIdentityCookie.getValue())),
                    "keycloak identity cookie should be provided");

                sessionResetted.flag();
                req.response().setStatusCode(200).end();
            })
            .listen(sessionResetPort, testCtx.succeeding(v -> sessionResetServerStarted.flag()));

        portalGateway(vertx, HOST, testCtx)
            .withRoutingContextHolder(routingContextHolder)
            .withSessionMiddleware()
            .withMockOAuth2Middleware()
            .withSessionBagMiddleware(List.of())
            .withMiddleware(prefilledSessionBag(routingContextHolder, keycloakIdentityCookie))
            .withControlApiMiddleware(ControlApiAction.SESSION_RESET, String.format("http://%s:%d/", HOST, sessionResetPort), WebClient.create(vertx))
            .withBackend(vertx, backendPort, cookieInsertionHandler)
            .withProxyMiddleware(backendPort)
            .build().start()
            // when
            .incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
                // then
                assertSessionReset(testCtx, outgoingResponse, routingContextHolder.get(), keycloakTestCookie);
                assertControlCookieRemoval(testCtx, outgoingResponse);
                responseReceived.flag();
            });
    }

    private void assertSessionReset(VertxTestContext testCtx, HttpClientResponse outgoingResponse, RoutingContext routingContext, Cookie keycloakCookie) {
        VertxAssertions.assertFalse(testCtx, routingContext.session().isDestroyed(), "session should not be destroyed");
        VertxAssertions.assertTrue(testCtx, AuthenticationUserContext.all(routingContext.session()).size() == 0,
            "there should be no oauth2 tokens in the session");

        VertxAssertions.assertEquals(testCtx, 200, outgoingResponse.statusCode(), "status code should be 200");
        VertxAssertions.assertFalse(testCtx,
            outgoingResponse.headers().getAll(HttpHeaders.SET_COOKIE.toString()).stream()
                .map(cookie -> ClientCookieDecoder.STRICT.decode(cookie))
                .anyMatch(cookie -> cookie.name().equals(CONTROL_COOKIE_NAME)),
            "control cookie should not appear in the response)");

        final Set<Cookie> cookiesInContext = routingContext.session().get(SESSION_BAG_COOKIES);
        VertxAssertions.assertEquals(testCtx, 2, cookiesInContext.size(),
            "there should be only one cookie in the session bag");
        VertxAssertions.assertTrue(testCtx, cookiesInContext.stream().anyMatch(cookie -> cookie.getName().startsWith("KEYCLOAK_")),
            "there should be the test keycloak cookie in the session bag");
    }

    private void assertControlCookieRemoval(VertxTestContext testCtx, HttpClientResponse response) {
        final List<String> cookies = response.cookies();
        final long controlCookieCount = cookies.stream()
            .filter(cookie -> cookie.startsWith(CONTROL_COOKIE_NAME))
            .count();
        VertxAssertions.assertTrue(testCtx, controlCookieCount == 0);
    }

    private Handler<RoutingContext> getCookieInsertionHandler(List<Cookie> cookies) {
        return ctx -> {
            for (Cookie cookie : cookies) {
                ctx.response().addCookie(cookie);
            }
            ctx.response().end();
        };
    }

    private Handler<RoutingContext> prefilledSessionBag(AtomicReference<RoutingContext> ctxHolder, Cookie keycloakIdentityCookie) {
        // use implementation knowledge because we cannot init the session bag with prefilled cookies
        return ctx -> {
            ctxHolder.get().session().put(SESSION_BAG_COOKIES, new HashSet<Cookie>(Set.of(keycloakIdentityCookie)));
            ctx.next();
        };
    }
}
