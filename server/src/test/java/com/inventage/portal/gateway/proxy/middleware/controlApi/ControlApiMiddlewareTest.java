package com.inventage.portal.gateway.proxy.middleware.controlApi;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware.CONTROL_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware.SESSION_RESET_ACTION;
import static com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware.SESSION_TERMINATE_ACTION;
import static com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware.SESSION_BAG_COOKIES;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ControlApiMiddlewareTest {

    private static final String HOST = "localhost";

    @Test
    void sessionTerminationTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final Cookie testCookie = Cookie.cookie("test-cookie", "value");
        testCookie.setMaxAge(3600);
        final Cookie sessionTerminateCookie = Cookie.cookie(CONTROL_COOKIE_NAME, SESSION_TERMINATE_ACTION);
        sessionTerminateCookie.setMaxAge(3600);
        final Checkpoint sessionTerminated = testCtx.checkpoint();
        final Checkpoint sessionTerminationServerStarted = testCtx.checkpoint();
        final Checkpoint responseReceived = testCtx.checkpoint();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> cookieInsertionHandler = getCookieInsertionHandler(List.of(testCookie, sessionTerminateCookie));

        final int sessionTerminationPort = TestUtils.findFreePort();
        vertx.createHttpServer()
            .requestHandler(req -> {
                sessionTerminated.flag();
                req.response().setStatusCode(200).end();
            })
            .listen(sessionTerminationPort, testCtx.succeeding(v -> sessionTerminationServerStarted.flag()));

        final WebClient webclient = WebClient.create(vertx, new WebClientOptions().setDefaultPort(sessionTerminationPort).setDefaultHost("localhost"));

        portalGateway(vertx, HOST, testCtx)
            .withRoutingContextHolder(routingContext)
            .withSessionMiddleware()
            .withMockOAuth2Middleware("localhost", sessionTerminationPort)
            .withControlApiMiddleware("SESSION_TERMINATE", webclient)
            .withSessionBagMiddleware(new JsonArray())
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cookieInsertionHandler)
            .build().start()
            // when
            .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.SET_COOKIE, "test-cookie=value;"), (outgoingResponse) -> {
                // then
                assertSessionTermination(outgoingResponse, routingContext.get());
                responseReceived.flag();
            });
    }

    private void assertSessionTermination(HttpClientResponse outgoingResponse, RoutingContext routingContext) {
        // TODO: Test endSessionUrl send action
        Assertions.assertEquals(200, outgoingResponse.statusCode(), "expected status code: 200");
        Assertions.assertTrue(routingContext.session().isDestroyed(), "session should be destroyed");
        Assertions.assertNull(outgoingResponse.headers().get("test-cookie"),
            "test-cookie should not appear in the response (all data deleted from session)");
        Assertions.assertEquals(routingContext.session().data(), new HashMap<>(),
            "all data should be deleted from session");
    }

    @Test
    void sessionResetTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final Cookie testCookie = Cookie.cookie("test-cookie", "value");
        testCookie.setMaxAge(3600);
        final Cookie keycloakTestCookie = Cookie.cookie("KEYCLOAK_TEST", "keycloak");
        keycloakTestCookie.setMaxAge(3600);
        final Cookie sessionResetCookie = Cookie.cookie(CONTROL_COOKIE_NAME, SESSION_RESET_ACTION);
        sessionResetCookie.setMaxAge(3600);
        final Checkpoint responseReceived = testCtx.checkpoint();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> cookieInsertionHandler = getCookieInsertionHandler(List.of(testCookie, keycloakTestCookie, sessionResetCookie));

        portalGateway(vertx, HOST, testCtx)
            .withRoutingContextHolder(routingContext)
            .withSessionMiddleware()
            .withMockOAuth2Middleware()
            .withControlApiMiddleware("SESSION_RESET", WebClient.create(vertx))
            .withSessionBagMiddleware(new JsonArray())
            .withBackend(vertx, backendPort, cookieInsertionHandler)
            .withProxyMiddleware(backendPort)
            .build().start()
            // when
            .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.SET_COOKIE, "test-cookie=value;"), (outgoingResponse) -> {
                // then
                assertSessionReset(outgoingResponse, routingContext.get(), keycloakTestCookie);
                responseReceived.flag();
            });
    }

    private void assertSessionReset(HttpClientResponse outgoingResponse, RoutingContext routingContext, Cookie keycloakCookie) {
        Assertions.assertEquals(200, outgoingResponse.statusCode(), "expected status code: 200");
        Assertions.assertFalse(routingContext.session().isDestroyed(), "session should not be destroyed");
        Assertions.assertNull(outgoingResponse.headers().get("test-cookie"),
            "test-cookie should not appear in the response");
        Assertions.assertEquals(Collections.emptyList(), AuthenticationUserContext.all(routingContext.session()),
            "there should be no session scope data in the session");
        final Set<Cookie> cookiesInContext = routingContext.session().get(SESSION_BAG_COOKIES);
        Assertions.assertEquals(1, cookiesInContext.size(),
            "there should be only one cookie in the session bag");
        Assertions.assertTrue(cookiesInContext.stream().anyMatch(cookie1 -> cookie1.getName().equals(keycloakCookie.getName())),
            "there should be the test keycloak cookie in the session bag");
    }

    private Handler<RoutingContext> getCookieInsertionHandler(List<Cookie> cookies) {
        return ctx -> {
            for (Cookie cookie : cookies) {
                ctx.response().addCookie(cookie);
            }
            ctx.response().end();
        };
    }
}
