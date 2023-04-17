package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import org.assertj.core.api.Assertions;
import org.assertj.core.error.AssertJMultipleFailuresError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware.SESSION_COOKIE_NAME_DEFAULT;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_SESSION_COOKIE_NAME;
import static io.vertx.ext.web.sstore.LocalSessionStore.DEFAULT_SESSION_MAP_NAME;

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
    public void sessionTimeoutNoReset(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withSessionMiddleware(List.of("/request2", "/request3")).build().start();
        BrowserConnected browser = gateway.connectBrowser();
        HeadersMultiMap headersMultiMap = new HeadersMultiMap();

        final List<String> sessionId = new ArrayList<>();
        final List<Long> lastAccessed = new ArrayList<>();
        // when
        browser.request(GET, "/request1")
            .whenComplete((response, error) -> {
                assertThat(response).hasStatusCode(200);
                vertx.getOrCreateContext();
                SharedDataSessionImpl sharedDataSession = vertx.sharedData().getLocalMap(DEFAULT_SESSION_MAP_NAME)
                        .values()
                        .stream()
                        .map(item -> (SharedDataSessionImpl) item)
                        .findFirst()
                        .orElseThrow();
                sessionId.add(sharedDataSession.id());
                lastAccessed.add(sharedDataSession.lastAccessed());

            })
            .thenCompose(response -> {
                final String sessionCookie = response.cookies().stream().filter(cookie -> cookie.startsWith(SESSION_COOKIE_NAME_DEFAULT)).findFirst().orElseThrow();
                System.out.println(SESSION_COOKIE_NAME_DEFAULT + "=" + sessionId.get(0));
                headersMultiMap.add("cookie", SESSION_COOKIE_NAME_DEFAULT + "=" + sessionId.get(0));
                System.out.println("session in response: " + sessionCookie);
                return browser.request(GET, "/request2", headersMultiMap);
            })
            .whenComplete((response, error) -> {
                // then
                System.out.println(response);

                assertThat(response)
                        .hasStatusCode(200);

                vertx.getOrCreateContext();
                SharedDataSessionImpl sharedDataSession = vertx.sharedData().getLocalMap(DEFAULT_SESSION_MAP_NAME)
                        .values()
                        .stream()
                        .map(item -> (SharedDataSessionImpl) item)
                        .findFirst()
                        .orElseThrow();
                Assertions.assertThat(sharedDataSession.id()).isEqualTo(sessionId.get(0));
                Assertions.assertThat(sharedDataSession.lastAccessed()).isEqualTo(lastAccessed.get(0));
                testCtx.completeNow();
            });
    }

}
