package com.inventage.portal.gateway.proxy.middleware.session;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.sstore.LocalSessionStore.DEFAULT_SESSION_MAP_NAME;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    private static Entry<Set<Function<String, String>>, Set<String>> entry(Set<Function<String, String>> a, Set<String> b) {
        return new AbstractMap.SimpleEntry<Set<Function<String, String>>, Set<String>>(a, b);
    }

    private static Stream<Entry<Set<Function<String, String>>, Set<String>>> provideCookieHeaders() {
        return Stream.of(
            entry(
                // only the session cookie
                Set.of(sc -> sc), // input, each element is a cookie header value
                Set.of() // expected, element line is an expected cookie
            ),
            entry(
                // session cookie and another cookie in separate headers
                Set.of(
                    sc -> "foo=canttouchthis",
                    sc -> sc),
                Set.of("foo=canttouchthis") //
            ),
            entry(
                // session cookie and other cookies in separate headers
                Set.of(
                    sc -> "foo=canttouchthis",
                    sc -> "bar=orthat",
                    sc -> sc),
                Set.of("foo=canttouchthis", "bar=orthat") //
            ),
            entry(
                // session cookie and another cookie in the same header (pre)
                Set.of(sc -> String.join("; ", sc, "foo=canttouchthis")),
                Set.of("foo=canttouchthis") //
            ),
            entry(
                // session cookie and another cookie in the same header (post)
                Set.of(sc -> String.join("; ", "foo=canttouchthis", sc)),
                Set.of("foo=canttouchthis") //
            ),
            entry(
                // session cookie and another cookie in the same header (middle)
                Set.of(sc -> String.join("; ", "foo=canttouchthis", sc, "bar=orthat")),
                Set.of("foo=canttouchthis", "bar=orthat") //
            ) //
        );
    }

    @ParameterizedTest
    @MethodSource("provideCookieHeaders")
    void sessionCookieIsNotPassedToTheBackendService(Entry<Set<Function<String, String>>, Set<String>> arg, Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final Set<Function<String, String>> cookieHeaders = arg.getKey();
        final Set<String> expectedCookies = arg.getValue();
        final AtomicInteger reqCount = new AtomicInteger();
        final int backendPort = TestUtils.findFreePort();

        Handler<RoutingContext> backendHandler = ctx -> {
            final int rc = reqCount.incrementAndGet();
            // then
            testCtx.verify(() -> {
                assertNull(ctx.request().getCookie(DEFAULT_SESSION_COOKIE_NAME),
                    String.format("session cookie was passed to the backend on the %d. request", rc));
                final List<String> cookies = ctx.request().headers().getAll("cookie");
                for (String ec : expectedCookies) {
                    assertTrue(
                        cookies.contains(ec),
                        String.format("colateral damage: other cookie than session cookie was removed from request, expected: %s, actual: %s",
                            expectedCookies,
                            ctx.request().cookies().stream().map(c -> c.getName()).toList()));
                }
            });
            ctx.response().setStatusCode(200).end("ok");
        };

        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start();

        final String emptySessionCookie = "";
        final RequestOptions reqOpts1 = new RequestOptions();
        for (Function<String, String> f : cookieHeaders) {
            final String cookieHeaderValue = f.apply(emptySessionCookie);
            if (cookieHeaderValue.length() != 0) {
                reqOpts1.addHeader(HttpHeaders.COOKIE, f.apply(emptySessionCookie));
            }
        }

        // when
        // obtain session cookie
        gateway.incomingRequest(GET, "/", reqOpts1, (outgoingResponse) -> {
            final String sessionCookie = outgoingResponse.cookies().stream()
                .filter(c -> c.startsWith(DEFAULT_SESSION_COOKIE_NAME))
                .map(c -> c.split(";")[0])
                .findFirst()
                .orElseThrow();

            final RequestOptions reqOpts2 = new RequestOptions();
            for (Function<String, String> f : cookieHeaders) {
                reqOpts2.addHeader(HttpHeaders.COOKIE, f.apply(sessionCookie));
            }

            // send session cookie
            gateway.incomingRequest(GET, "/", reqOpts2, (outgoingResponse2) -> {
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
