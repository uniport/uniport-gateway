package com.inventage.portal.gateway.proxy.middleware.session;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.sstore.LocalSessionStore.DEFAULT_SESSION_MAP_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
            assertThat(testCtx, response)
                .hasSetCookie(DEFAULT_SESSION_LIFETIME_COOKIE_NAME);
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
            assertThat(testCtx, response)
                .hasHeader(DEFAULT_SESSION_LIFETIME_HEADER_NAME)
                .hasNotSetCookie(DEFAULT_SESSION_LIFETIME_COOKIE_NAME);
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
            assertThat(testCtx, response)
                .hasStatusCode(200)
                .hasSetSessionCookie(null);
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
                assertThat(testCtx, response).hasStatusCode(200);
                return browser.request(GET, "/request2");
            })
            .thenCompose(response -> browser.request(POST, "/request3"))
            .whenComplete((response, error) -> {
                // then
                assertThat(testCtx, response)
                    .hasStatusCode(200)
                    .hasSetSessionCookie(null);
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
                assertThat(testCtx, response).hasStatusCode(200);
                vertx.getOrCreateContext();
                SharedDataSessionImpl sharedDataSession = getSharedDataSession(vertx);
                sessionId.add(sharedDataSession.id());
                lastAccessed.add(sharedDataSession.lastAccessed());
            })
            .thenCompose(response -> {
                headersMultiMap.add("cookie", DEFAULT_SESSION_COOKIE_NAME + "=" + sessionId.get(0));
                return browser.request(GET, "/request2?key=value", headersMultiMap);
            })
            .whenComplete((response, error) -> {
                // then
                assertThat(testCtx, response).hasStatusCode(200);
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

    @Test
    public void shouldSetSessionIdleTimeoutOnRoutingContext(Vertx vertx, VertxTestContext testCtx) {
        // given 
        final AtomicReference<Boolean> hasSessionIdleTimeout = new AtomicReference<>(false);

        Handler<RoutingContext> checkSessionIdleTimeoutIsOnRoutingContext = ctx -> {
            hasSessionIdleTimeout.set(ctx.get(SessionMiddleware.SESSION_MIDDLEWARE_IDLE_TIMEOUT_IN_MS_KEY) != null);
            ctx.next();
        };

        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(checkSessionIdleTimeoutIsOnRoutingContext)
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
            // then
            Assertions.assertThat(hasSessionIdleTimeout.get());
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldSetSessionStoreOnRoutingContext(Vertx vertx, VertxTestContext testCtx) {
        // given 
        final AtomicReference<Boolean> hasSessionStore = new AtomicReference<>(false);

        Handler<RoutingContext> checkSessionStoreIsOnRoutingContext = ctx -> {
            hasSessionStore.set(ctx.get(SessionMiddleware.SESSION_MIDDLEWARE_SESSION_STORE_KEY) != null);
            ctx.next();
        };

        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(checkSessionStoreIsOnRoutingContext)
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
            // then
            Assertions.assertThat(hasSessionStore.get());
            testCtx.completeNow();
        });
    }

    @Test
    void sessionCookieIsNotPassedToTheBackendService(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final AtomicInteger reqCount = new AtomicInteger();
        final int backendPort = TestUtils.findFreePort();
        final String dummyCookie = "dummy=canttouchthis";

        Handler<RoutingContext> backendHandler = ctx -> {
            final int rc = reqCount.incrementAndGet();
            // then
            testCtx.verify(() -> {
                assertNull(ctx.request().getCookie(DEFAULT_SESSION_COOKIE_NAME),
                    String.format("session cookie was passed to the backend on the %d. request", rc));
                assertNotNull(ctx.request().getCookie("dummy"),
                    "colateral damage: other cookie than session cookie was removed from request");
            });
            ctx.response().setStatusCode(200).end("ok");
        };

        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start();

        // when
        // obtain session cookie
        gateway.incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.COOKIE, dummyCookie), (outgoingResponse) -> {
            final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
            for (String respCookie : outgoingResponse.cookies()) {
                headers.add(HttpHeaders.COOKIE, respCookie);
            }
            headers.add(HttpHeaders.COOKIE, "dummy=canttouchthis");

            // send session cookie
            final RequestOptions opts = new RequestOptions().setHeaders(headers);
            gateway.incomingRequest(GET, "/", opts, (outgoingResponse2) -> {
                testCtx.completeNow();
            });
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
