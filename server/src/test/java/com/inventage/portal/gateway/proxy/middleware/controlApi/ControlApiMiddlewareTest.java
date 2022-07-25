package com.inventage.portal.gateway.proxy.middleware.controlApi;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware.*;
import static com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware.SESSION_BAG_COOKIES;

@ExtendWith(VertxExtension.class)
public class ControlApiMiddlewareTest {

    // necessary for jaeger (OpenTracing)
    static {
        System.setProperty("JAEGER_SERVICE_NAME", "portal-gateway");
    }

    private static final String host = "localhost";

    private int port;

    @BeforeEach
    public void setup() {
        port = TestUtils.findFreePort();
    }

    @Test
    void sessionTerminationTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final io.vertx.core.http.Cookie testCookie = new CookieImpl("test-cookie", "value");
        testCookie.setMaxAge(3600);
        final io.vertx.core.http.Cookie sessionTerminateCookie = new CookieImpl(CONTROL_COOKIE_NAME, SESSION_TERMINATE_ACTION);
        sessionTerminateCookie.setMaxAge(3600);
        final Checkpoint responseReceived = testCtx.checkpoint();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> cookieInsertionHandler = getCookieInsertionHandler(List.of(testCookie, sessionTerminateCookie));

        portalGateway(vertx, host, port)
                .withRoutingContextHolder(routingContext)
                .withMockOAuth2Middleware()
                .withSessionBagMiddleware(new JsonArray())
                .withControlApiMiddleware("SESSION_TERMINATE")
                .withProxyMiddleware(backendPort)
                .withBackend(vertx, backendPort, cookieInsertionHandler)
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.SET_COOKIE, "test-cookie=value;"), (outgoingResponse) -> {
                    // then
                    assertSessionTermination(outgoingResponse, routingContext.get(), testCookie);
                    responseReceived.flag();
                });
    }

    private void assertSessionTermination(HttpClientResponse outgoingResponse, RoutingContext routingContext, io.vertx.core.http.Cookie testCookie) {
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
        final io.vertx.core.http.Cookie testCookie = new CookieImpl("test-cookie", "value");
        testCookie.setMaxAge(3600);
        final io.vertx.core.http.Cookie keycloakTestCookie = new CookieImpl("KEYCLOAK_TEST", "keycloak");
        keycloakTestCookie.setMaxAge(3600);
        final io.vertx.core.http.Cookie sessionResetCookie = new CookieImpl(CONTROL_COOKIE_NAME, SESSION_RESET_ACTION);
        sessionResetCookie.setMaxAge(3600);
        final Checkpoint responseReceived = testCtx.checkpoint();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> cookieInsertionHandler = getCookieInsertionHandler(List.of(testCookie, keycloakTestCookie, sessionResetCookie));


        portalGateway(vertx, host, port)
                .withRoutingContextHolder(routingContext)
                .withMockOAuth2Middleware()
                .withSessionBagMiddleware(new JsonArray())
                .withControlApiMiddleware("SESSION_RESET")
                .withBackend(vertx, backendPort, cookieInsertionHandler)
                .withProxyMiddleware(backendPort)
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.SET_COOKIE, "test-cookie=value;"), (outgoingResponse) -> {
                    // then
                    assertSessionReset(outgoingResponse, routingContext.get(), keycloakTestCookie);
                    responseReceived.flag();
                });
    }

    private void assertSessionReset(HttpClientResponse outgoingResponse, RoutingContext routingContext, io.vertx.core.http.Cookie keycloakCookie) {
        Assertions.assertEquals(200, outgoingResponse.statusCode(), "expected status code: 200");
        Assertions.assertFalse(routingContext.session().isDestroyed(), "session should not be destroyed");
        Assertions.assertNull(outgoingResponse.headers().get("test-cookie"),
                "test-cookie should not appear in the response");
        Assertions.assertEquals(Collections.emptyList(), filterSessionScopesFrom(routingContext.session().data()),
                "there should be no session scope data in the session");
        Set<io.netty.handler.codec.http.cookie.DefaultCookie> cookiesInContext = routingContext.session().get(SESSION_BAG_COOKIES);
        Assertions.assertEquals(1, cookiesInContext.size(),
                "there should be only one cookie in the session bag");
        Assertions.assertTrue(cookiesInContext.stream().anyMatch(cookie1 -> cookie1.name().equals(keycloakCookie.getName())),
                "there should be the test keycloak cookie in the session bag");
    }

    private List<String> filterSessionScopesFrom(Map<String, Object> contextData) {
        return contextData.keySet().stream()
                .filter(key -> key.endsWith(OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX))
                .collect(Collectors.toList());
    }

    private Handler<RoutingContext> getCookieInsertionHandler(List<Cookie> cookies) {
        return ctx -> {
            for (Cookie cookie: cookies) {
                ctx.response().addCookie(cookie);
            }
            ctx.response().end();
        };
    }
}
