package com.inventage.portal.gateway.proxy.middleware.session;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware.SESSION_COOKIE_NAME_DEFAULT;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware.SESSION_LIFETIME_COOKIE_NAME_DEFAULT;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.sstore.LocalSessionStore.DEFAULT_SESSION_MAP_NAME;

import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
                .hasSetCookie(SESSION_LIFETIME_COOKIE_NAME_DEFAULT);
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
                .hasNotSetCookie(SESSION_LIFETIME_COOKIE_NAME_DEFAULT);
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
            .withSessionMiddleware("^/(request2|request3).*").build().start();
        BrowserConnected browser = gateway.connectBrowser();
        HeadersMultiMap headersMultiMap = new HeadersMultiMap();

        final List<String> sessionId = new ArrayList<>();
        final List<Long> lastAccessed = new ArrayList<>();
        // when
        browser.request(GET, "/request1")
            .whenComplete((response, error) -> {
                assertThat(response).hasStatusCode(200);
                vertx.getOrCreateContext();
                SharedDataSessionImpl sharedDataSession = getSharedDataSession(vertx);
                sessionId.add(sharedDataSession.id());
                lastAccessed.add(sharedDataSession.lastAccessed());
            })
            .thenCompose(response -> {
                headersMultiMap.add("cookie", SESSION_COOKIE_NAME_DEFAULT + "=" + sessionId.get(0));
                return browser.request(GET, "/request2?key=value", headersMultiMap);
            })
            .whenComplete((response, error) -> {
                // then
                assertThat(response).hasStatusCode(200);
                vertx.getOrCreateContext();
                SharedDataSessionImpl sharedDataSession = getSharedDataSession(vertx);
                Assertions.assertThat(sharedDataSession.id()).isEqualTo(sessionId.get(0));
                Assertions.assertThat(sharedDataSession.lastAccessed()).isEqualTo(lastAccessed.get(0));
                testCtx.completeNow();
            });
    }

    @Test
    public void noResetRequestDoesNotAccessCookie(Vertx vertx, VertxTestContext testCtx) {
        // given
        final AtomicReference<String> originalCookie = new AtomicReference<>();

        BrowserConnected browser = portalGateway(vertx, testCtx)
            .withSessionMiddleware("^/(ignored).*", true, true)
            .build().start().connectBrowser();

        // when
        browser.request(GET, "/request")
            .thenCompose(response -> browser.request(GET, "/ignored"))
            .whenComplete((response, error) -> {
                vertx.getOrCreateContext();
                final String cookie = response.cookies().get(0);
                originalCookie.set(cookie);
            })
            .thenCompose(response -> browser.request(GET, "/ignored"))
            .whenComplete((response, error) -> {
                // then
                vertx.getOrCreateContext();
                final String cookie = response.cookies().get(0);
                Assertions.assertThat(originalCookie.get()).isEqualTo(cookie);
                testCtx.completeNow();
            });
    }

    private SharedDataSessionImpl getSharedDataSession(Vertx vertx) {
        return vertx.sharedData().getLocalMap(DEFAULT_SESSION_MAP_NAME)
            .values()
            .stream()
            .map(item -> (SharedDataSessionImpl) item)
            .findFirst()
            .orElseThrow();
    }
}
