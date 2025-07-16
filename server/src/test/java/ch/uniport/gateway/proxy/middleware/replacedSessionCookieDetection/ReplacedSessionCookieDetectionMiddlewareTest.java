package ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection.AbstractReplacedSessionCookieDetectionMiddlewareOptions.DEFAULT_DETECTION_COOKIE_NAME;
import static ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection.AbstractReplacedSessionCookieDetectionMiddlewareOptions.DEFAULT_MAX_REDIRECT_RETRIES;
import static ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection.AbstractReplacedSessionCookieDetectionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME;
import static io.vertx.core.http.HttpMethod.GET;

import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import ch.uniport.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class ReplacedSessionCookieDetectionMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacedSessionCookieDetectionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ReplacedSessionCookieDetectionMiddlewareFactory.DETECTION_COOKIE_NAME,
                        "uniport-test.state",
                        ReplacedSessionCookieDetectionMiddlewareFactory.WAIT_BEFORE_RETRY_MS, 4242,
                        ReplacedSessionCookieDetectionMiddlewareFactory.MAX_REDIRECT_RETRIES, 42)))));

        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacedSessionCookieDetectionMiddlewareFactory.TYPE)));

        final JsonObject invalidTimeout = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacedSessionCookieDetectionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ReplacedSessionCookieDetectionMiddlewareFactory.WAIT_BEFORE_RETRY_MS, -1)))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacedSessionCookieDetectionMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacedSessionCookieDetectionMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept valid config", simple, complete, expectedTrue),
            Arguments.of("accept minimal config", minimal, complete, expectedTrue),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse),
            Arguments.of("reject config with invalid timeout", invalidTimeout, complete, expectedFalse)

        );
    }

    @Test
    public void shouldSetDetectionCookieOnResponse(Vertx vertx, VertxTestContext testCtx) {
        final AtomicLong sessionLastAccessedHolder = new AtomicLong();
        final Handler<RoutingContext> extractSessionLastAccessed = ctx -> {
            // we need to do that after the middleware has accessed the session and
            // before the session middleware accesses the session to set the cookie
            // to get the exact same timestamp
            ctx.addHeadersEndHandler(v -> sessionLastAccessedHolder.set(ctx.session().lastAccessed()));
            ctx.next();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(extractSessionLastAccessed)
            .withUser()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
            // then
            final long lastAccessed = sessionLastAccessedHolder.get();
            final long sessionLifetimeMs = AbstractSessionMiddlewareOptions.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE * 60
                * 1000;
            final DetectionCookieValue detectionCookieValue = new DetectionCookieValue(lastAccessed, sessionLifetimeMs);

            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(200)
                .hasSetCookie(DEFAULT_DETECTION_COOKIE_NAME, detectionCookieValue.toString());
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldRedirect(Vertx vertx, VertxTestContext testCtx) {
        // given
        final long validTimestamp = System.currentTimeMillis() + 60_000;
        final Set<Cookie> cookies = new HashSet<Cookie>();

        cookies.add(createSessionCookie("a-session-id-which-has-been-replaced"));
        cookies.add(createDetectionCookie(0, validTimestamp));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final String encodedCookies = ClientCookieEncoder.STRICT.encode(cookies);
        if (encodedCookies != null) {
            headers.add(HttpHeaders.COOKIE, encodedCookies);
        }

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(307)
                .hasNotSetSessionCookie();
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirectWhenSessionIsRegenerated(Vertx vertx, VertxTestContext testCtx) {
        // given
        final long expiredTimestamp = System.currentTimeMillis() - 1000;
        final Set<Cookie> cookies = new HashSet<Cookie>();

        cookies.add(createSessionCookie("=a-session-id-which-has-been-replaced"));
        cookies.add(createDetectionCookie(0, expiredTimestamp));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final String encodedCookies = ClientCookieEncoder.STRICT.encode(cookies);
        if (encodedCookies != null) {
            headers.add(HttpHeaders.COOKIE, encodedCookies);
        }

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(200)
                .hasSetSessionCookie(null);
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirectWhenUniportStateCookieIsMissing(Vertx vertx, VertxTestContext testCtx) {
        // given
        final Set<Cookie> cookies = new HashSet<Cookie>();
        cookies.add(createSessionCookie("=a-session-id-which-has-been-replaced"));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final String encodedCookies = ClientCookieEncoder.STRICT.encode(cookies);
        if (encodedCookies != null) {
            headers.add(HttpHeaders.COOKIE, encodedCookies);
        }

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(200)
                .hasSetSessionCookie(null);
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirectWhenUniportStateCookieIsOutdated(Vertx vertx, VertxTestContext testCtx) {
        // given
        final Set<Cookie> cookies = new HashSet<Cookie>();
        final long expiredTimestamp = System.currentTimeMillis() - 1000;

        cookies.add(createSessionCookie("=a-session-id-which-has-been-replaced"));
        cookies.add(createDetectionCookie(DEFAULT_MAX_REDIRECT_RETRIES - 1, expiredTimestamp));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final String encodedCookies = ClientCookieEncoder.STRICT.encode(cookies);
        if (encodedCookies != null) {
            headers.add(HttpHeaders.COOKIE, encodedCookies);
        }

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(200)
                .hasSetSessionCookie(null);
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirectWhenUniportStateCookieIsOutOfCounter(Vertx vertx, VertxTestContext testCtx) {
        // given
        final long validTimestamp = System.currentTimeMillis() + 60_000;
        final Set<Cookie> cookies = new HashSet<Cookie>();

        cookies.add(createSessionCookie("=a-session-id-which-has-been-replaced"));
        cookies.add(createDetectionCookie(DEFAULT_MAX_REDIRECT_RETRIES, validTimestamp));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final String encodedCookies = ClientCookieEncoder.STRICT.encode(cookies);
        if (encodedCookies != null) {
            headers.add(HttpHeaders.COOKIE, encodedCookies);
        }

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .hasStatusCode(200)
                .hasSetSessionCookie(null);
            testCtx.completeNow();
        });
    }

    private Cookie createSessionCookie(String value) {
        return new DefaultCookie(DEFAULT_SESSION_COOKIE_NAME, value);
    }

    private Cookie createDetectionCookie(int retries, long validUntilUnixTimestamp) {
        return new DefaultCookie(
            DEFAULT_DETECTION_COOKIE_NAME,
            new DetectionCookieValue(retries + DetectionCookieValue.SPLITTER + (validUntilUnixTimestamp / 1000)).toString());
    }

}
