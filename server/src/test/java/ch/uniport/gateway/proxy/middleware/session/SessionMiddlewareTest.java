package ch.uniport.gateway.proxy.middleware.session;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static ch.uniport.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME;
import static ch.uniport.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions.DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
import static ch.uniport.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions.DEFAULT_SESSION_LIFETIME_HEADER_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.sstore.LocalSessionStore.DEFAULT_SESSION_MAP_NAME;
import static org.junit.jupiter.api.Assertions.assertNull;

import ch.uniport.gateway.TestUtils;
import ch.uniport.gateway.proxy.middleware.BrowserConnected;
import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import ch.uniport.gateway.proxy.middleware.VertxAssertions;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class SessionMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware(
                    "sessionMiddleware", SessionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES, 15,
                        SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH, 32,
                        SessionMiddlewareFactory.NAG_HTTPS, true,
                        SessionMiddlewareFactory.IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, "^/polling/.*",
                        SessionMiddlewareFactory.SESSION_COOKIE, JsonObject.of(
                            SessionMiddlewareFactory.SESSION_COOKIE_NAME, "uniport.session",
                            SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY, true,
                            SessionMiddlewareFactory.SESSION_COOKIE_SECURE, false,
                            SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE, "STRICT"))))));

        final JsonObject invalidTimeout = buildConfiguration(
            withMiddlewares(
                withMiddleware(
                    "sessionMiddleware", SessionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES, -1)))));

        final JsonObject invalidIdLength = buildConfiguration(
            withMiddlewares(
                withMiddleware(
                    "sessionMiddleware", SessionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH, -1)))));

        final JsonObject invalidCookieSameSite = buildConfiguration(
            withMiddlewares(
                withMiddleware(
                    "sessionMiddleware", SessionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionMiddlewareFactory.SESSION_COOKIE, JsonObject.of(
                            SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE, "blub"))))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with invalid timeout", invalidTimeout, complete, expectedFalse),
            Arguments.of("reject config with invalid id length", invalidIdLength, complete, expectedFalse),
            Arguments.of("reject config with invalid cookie same site", invalidCookieSameSite, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    public void sessionLifetimeCookie(Vertx vertx, VertxTestContext testCtx) {
        // given
        final BrowserConnected browser = portalGateway(vertx, testCtx)
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
        final BrowserConnected browser = portalGateway(vertx, testCtx)
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
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .build().start();
        final BrowserConnected browser = gateway.connectBrowser();
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
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware().build().start();
        final BrowserConnected browser = gateway.connectBrowser();
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
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware("^/(request2|request3).*").build().start();
        final BrowserConnected browser = gateway.connectBrowser();
        final HeadersMultiMap headersMultiMap = new HeadersMultiMap();

        final List<String> sessionId = new ArrayList<>();
        final List<Long> lastAccessed = new ArrayList<>();
        // when
        browser.request(GET, "/request1")
            .whenComplete((response, error) -> {
                assertThat(testCtx, response).hasStatusCode(200);
                vertx.getOrCreateContext();
                final SharedDataSessionImpl sharedDataSession = getSharedDataSession(vertx);
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
                final SharedDataSessionImpl sharedDataSession = getSharedDataSession(vertx);
                VertxAssertions.assertEquals(testCtx, sharedDataSession.id(), sessionId.get(0));
                VertxAssertions.assertEquals(testCtx, sharedDataSession.lastAccessed(), lastAccessed.get(0));
                testCtx.completeNow();
            });
    }

    @Test
    public void noResetRequestDoesNotAccessCookie(Vertx vertx, VertxTestContext testCtx) {
        // given
        final AtomicReference<String> originalCookie = new AtomicReference<>();

        final BrowserConnected browser = portalGateway(vertx, testCtx)
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
                VertxAssertions.assertEquals(testCtx, originalCookie.get(), cookie);
                testCtx.completeNow();
            });
    }

    @Test
    public void shouldSetSessionIdleTimeoutOnRoutingContext(Vertx vertx, VertxTestContext testCtx) {
        // given 
        final AtomicReference<Boolean> hasSessionIdleTimeout = new AtomicReference<>(false);

        final Handler<RoutingContext> checkSessionIdleTimeoutIsOnRoutingContext = ctx -> {
            hasSessionIdleTimeout.set(ctx.get(SessionMiddleware.SESSION_MIDDLEWARE_IDLE_TIMEOUT_IN_MS_KEY) != null);
            ctx.next();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(checkSessionIdleTimeoutIsOnRoutingContext)
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
            // then
            VertxAssertions.assertNotNull(testCtx, hasSessionIdleTimeout.get());
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldSetSessionStoreOnRoutingContext(Vertx vertx, VertxTestContext testCtx) {
        // given 
        final AtomicReference<Boolean> hasSessionStore = new AtomicReference<>(false);

        final Handler<RoutingContext> checkSessionStoreIsOnRoutingContext = ctx -> {
            hasSessionStore.set(ctx.get(SessionMiddleware.SESSION_MIDDLEWARE_SESSION_STORE_KEY) != null);
            ctx.next();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(checkSessionStoreIsOnRoutingContext)
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
            // then
            VertxAssertions.assertNotNull(testCtx, hasSessionStore.get());
            testCtx.completeNow();
        });
    }

    private static Entry<Set<Function<String, String>>, Set<String>> entry(Set<Function<String, String>> a, Set<String> b) {
        return new AbstractMap.SimpleEntry<Set<Function<String, String>>, Set<String>>(a, b);
    }

    private static Stream<Entry<Set<Function<String, String>>, Set<String>>> provideCookieHeadersContainingSessionCookie() {
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
    @MethodSource("provideCookieHeadersContainingSessionCookie")
    void shouldNotPassSessionCookieToABackendService(Entry<Set<Function<String, String>>, Set<String>> arg, Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final Set<Function<String, String>> cookieHeaders = arg.getKey();
        final Set<String> expectedCookies = arg.getValue();
        final AtomicInteger reqCount = new AtomicInteger();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> backendHandler = ctx -> {
            final int rc = reqCount.incrementAndGet();
            if (rc == 1) {
                // first request is for obtaining the session cookie
                ctx.response().setStatusCode(200).end("ok");
                return;
            }

            // then
            testCtx.verify(() -> {
                assertNull(ctx.request().getCookie(DEFAULT_SESSION_COOKIE_NAME),
                    String.format("session cookie was passed to the backend on the %d. request", rc));

                final Set<String> actualCookies = ctx.request()
                    .headers()
                    .getAll(HttpHeaders.COOKIE)
                    .stream()
                    .flatMap(c -> Arrays.stream(c.split(";")))
                    .map(c -> c.trim())
                    .collect(Collectors.toSet());
                Assertions.assertThat(actualCookies).isEqualTo(expectedCookies);
            });
            ctx.response().setStatusCode(200).end("ok");
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start();

        // obtain session cookie
        gateway.incomingRequest(GET, "/", (outgoingResponse) -> {
            final String sessionCookie = outgoingResponse.cookies().stream()
                .filter(c -> c.startsWith(DEFAULT_SESSION_COOKIE_NAME))
                .map(c -> c.split(";")[0])
                .findFirst()
                .orElseThrow();

            // when
            final RequestOptions reqOpts = new RequestOptions();
            for (Function<String, String> f : cookieHeaders) {
                reqOpts.addHeader(HttpHeaders.COOKIE, f.apply(sessionCookie));
            }

            // send session cookie
            gateway.incomingRequest(GET, "/", reqOpts, (outgoingResponse2) -> {
                testCtx.completeNow();
            });
        });
    }

    private static Stream<Arguments> provideCookieHeaders() {
        return Stream.of(
            Arguments.of(
                List.of(
                    List.of("a=1"))),
            Arguments.of(
                List.of(
                    List.of("a=1"),
                    List.of("b=2"))),
            Arguments.of(
                List.of(
                    List.of("a=1"),
                    List.of("b=2"),
                    List.of("c=3"))),
            Arguments.of(
                List.of(
                    List.of("a=1", "b=2"))),
            Arguments.of(
                List.of(
                    List.of("a=1", "b=2", "c=3"))),
            Arguments.of(
                List.of(
                    List.of("a=1", "b=2"),
                    List.of("c=3"))),
            Arguments.of(
                List.of(
                    List.of("a=1"),
                    List.of("b=2", "c=3")))

        );
    }

    @ParameterizedTest
    @MethodSource("provideCookieHeaders")
    void shouldNotChangeCookieHeaderStructure(List<List<String>> cookieHeaders, Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> backendHandler = ctx -> {
            // then
            testCtx.verify(() -> {
                final List<List<String>> actualCookies = ctx.request()
                    .headers()
                    .getAll(HttpHeaders.COOKIE)
                    .stream()
                    .map(cookieHeader -> Arrays.stream(cookieHeader.split(";"))
                        .map(cookie -> cookie.trim())
                        .toList())
                    .toList();
                Assertions.assertThat(actualCookies).isEqualTo(cookieHeaders);
            });

            ctx.response().setStatusCode(200).end("ok");
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start();

        // when
        final RequestOptions reqOpts = new RequestOptions();
        for (List<String> cookieHeader : cookieHeaders) {
            reqOpts.addHeader(HttpHeaders.COOKIE, String.join(";", cookieHeader));
        }
        gateway.incomingRequest(GET, "/", reqOpts, (outgoingResponse) -> {
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

    @Test
    public void shouldHandleMalformedCookiePORTAL2380(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final int backendPort = TestUtils.findFreePort();
        final String cookieName = "whatsNew:1.01";
        final MultiMap headers = HeadersMultiMap.httpHeaders()
            .set(HttpHeaders.COOKIE, cookieName + "=true"); // malformed cookie: contains illegal column
        final BrowserConnected browser = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, ctx -> {
                // then
                final Set<io.netty.handler.codec.http.cookie.Cookie> nettyCookies = ctx.request().headers().getAll(HttpHeaders.COOKIE).stream()
                    .filter(header -> header != null)
                    .flatMap(header -> ServerCookieDecoder.LAX.decode(header).stream())
                    .filter(cookie -> cookie != null)
                    .collect(Collectors.toSet());
                VertxAssertions.assertTrue(testCtx, nettyCookies.stream().anyMatch(cookie -> cookie.name().equals(cookieName)));
                ctx.end("ok");
            })
            .build().start()
            .connectBrowser();

        // when
        browser.request(GET, "/", headers)
            .whenComplete((response, error) -> {
                // then
                assertThat(testCtx, response)
                    .hasStatusCode(HttpResponseStatus.OK.code());
                testCtx.completeNow();
            });
    }
}
