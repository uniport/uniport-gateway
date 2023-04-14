package com.inventage.portal.gateway.proxy.middleware.session;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware.SESSION_COOKIE_NAME_DEFAULT;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_SESSION_COOKIE_NAME;

import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ExtendWith(VertxExtension.class)
public class SessionMiddlewareTest {

    @Test
    public void sessionLifetimeCookie(Vertx vertx, VertxTestContext testCtx) {
        // given
        BrowserConnected browser = portalGateway(vertx, testCtx)
            .withSessionMiddleware(false, true)
            .build().start().connectBrowser();
        // when
        browser.request(GET, "/").whenComplete((response, error) -> {
            // then
            assertThat(response)
                .hasSetCookie(SessionMiddleware.SESSION_LIFETIME_COOKIE_NAME_DEFAULT);
            testCtx.completeNow();
        });
    }

    @Test
    public void sessionLifetimeHeader(Vertx vertx, VertxTestContext testCtx) {
        // given
        BrowserConnected browser = portalGateway(vertx, testCtx)
            .withSessionMiddleware(true, false)
            .build().start().connectBrowser();
        // when
        browser.request(GET, "/").whenComplete((response, error) -> {
            // then
            assertThat(response)
                .hasHeader(SessionMiddleware.SESSION_LIFETIME_HEADER_NAME_DEFAULT)
                .hasNotSetCookie(SessionMiddleware.SESSION_LIFETIME_COOKIE_NAME_DEFAULT);
            testCtx.completeNow();
        });
    }

    @Test
    public void newSessionIsCreated(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .build().start();
        BrowserConnected browser = gateway.connectBrowser();
        // when
        browser.request(GET, "/").whenComplete((response, error) -> {
            // then
            assertThat(response)
                .hasStatusCode(200)
                .hasSetCookieForSession(null);
            testCtx.completeNow();
        });
    }

    @Test
    public void newSessionIsCreated2(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware().build().start();
        BrowserConnected browser = gateway.connectBrowser();
        // when
        browser.request(GET, "/request1")
            .thenCompose(response -> {
                assertThat(response).hasStatusCode(200);
                return browser.request(GET, "/request2");
            })
            .thenCompose(response -> browser.request(POST, "/request3"))
            .whenComplete((response, error) -> {
                // then
                assertThat(response)
                    .hasStatusCode(200)
                    .hasSetCookieForSession(null);
                testCtx.completeNow();
            });
    }

    @Test
    public void sessionTimeoutNotReset(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withSessionMiddleware().build().start();
        BrowserConnected browser = gateway.connectBrowser();
        HeadersMultiMap headersMultiMap = new HeadersMultiMap();
        final String sessionId = "";
        // when
        browser.request(GET, "/request1")
                .whenComplete((response, error) -> {
                    assertThat(response).hasStatusCode(200);
                    vertx.getOrCreateContext();
                    List<SharedDataSessionImpl> sessionDataList = vertx.sharedData().getLocalMap("vertx-web.sessions")
                            .values()
                            .stream()
                            .map(item -> (SharedDataSessionImpl) item).collect(Collectors.toList());
                    sessionDataList.forEach(item -> System.out.println(item.id() + ": " + item.lastAccessed()));
//                    sessionId = sessionDataList.stream().findFirst().get().id();

                })
                .thenCompose(response -> {
                    final String sessionCookie = response.cookies().stream().filter(cookie -> cookie.startsWith(SESSION_COOKIE_NAME_DEFAULT)).findFirst().orElseThrow();
                    headersMultiMap.add("cookie", response.getHeader("set-cookie"));
                    System.out.println("session in response: " + sessionCookie);
                    return browser.request(GET, "/request2", headersMultiMap);
                })
                .whenComplete((response, error) -> {
                    // then
                    System.out.println(response);

                    assertThat(response)
                            .hasStatusCode(200);
//                            .hasSetCookieForSession(headersMultiMap.get("cookie"));
                    vertx.getOrCreateContext();
                    List<SharedDataSessionImpl> sessionDataList = vertx.sharedData().getLocalMap("vertx-web.sessions")
                            .values()
                            .stream()
                            .map(item -> (SharedDataSessionImpl) item).collect(Collectors.toList());
                    sessionDataList.forEach(item -> System.out.println(item.id() + ": " + item.lastAccessed()));
                    testCtx.completeNow();
                });
    }

}
