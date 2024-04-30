package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.DetectionCookieValue.SPLITTER;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_DETECTION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_MAX_REDIRECT_RETRIES;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ReplacedSessionCookieDetectionMiddlewareTest {
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

        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(extractSessionLastAccessed)
            .withUser()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions(), (outgoingResponse) -> {
            // then
            final long lastAccessed = sessionLastAccessedHolder.get();
            final long sessionLifetimeMs = SessionMiddlewareFactory.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE * 60 * 1000;
            final DetectionCookieValue detectionCookieValue = new DetectionCookieValue(lastAccessed, sessionLifetimeMs);

            assertThat(outgoingResponse)
                .hasStatusCode(200)
                .hasSetCookie(DEFAULT_DETECTION_COOKIE_NAME, detectionCookieValue.toString());
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldRedirect(Vertx vertx, VertxTestContext testCtx) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_SESSION_COOKIE_NAME + "=a-session-id-which-has-been-replaced");
        long validTimestamp = System.currentTimeMillis() + 60_000;
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + createDetectionCookieString(0, validTimestamp));
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(307)
                .hasNotSetCookieForSession();
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirect_when_session_is_regenerated(Vertx vertx, VertxTestContext testCtx) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_SESSION_COOKIE_NAME + "=a-session-id-which-has-been-replaced");
        long expiredTimestamp = System.currentTimeMillis() - 1000;
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + createDetectionCookieString(0, expiredTimestamp));
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(200)
                .hasSetCookieForSession(null);
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirect_when_uniport_state_cookie_is_missing(Vertx vertx, VertxTestContext testCtx) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_SESSION_COOKIE_NAME + "=a-session-id-which-has-been-replaced");
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(200)
                .hasSetCookieForSession(null);
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirect_when_uniport_state_cookie_is_outdated(Vertx vertx, VertxTestContext testCtx) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_SESSION_COOKIE_NAME + "=a-session-id-which-has-been-replaced");
        long expiredTimestamp = System.currentTimeMillis() - 1000;
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + createDetectionCookieString(DEFAULT_MAX_REDIRECT_RETRIES - 1, expiredTimestamp));
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(200)
                .hasSetCookieForSession(null);
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldNotRedirect_when_uniport_state_cookie_is_out_of_counter(Vertx vertx, VertxTestContext testCtx) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_SESSION_COOKIE_NAME + "=a-session-id-which-has-been-replaced");
        long validTimestamp = System.currentTimeMillis() + 60_000;
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + createDetectionCookieString(DEFAULT_MAX_REDIRECT_RETRIES, validTimestamp));
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withResponseSessionCookieRemovalMiddleware()
            .withSessionMiddleware()
            .withReplacedSessionCookieDetectionMiddleware()
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(200)
                .hasSetCookieForSession(null);
            testCtx.completeNow();
        });
    }

    private String createDetectionCookieString(int retries, long validUntilUnixTimestamp) {
        return "=" + new DetectionCookieValue(retries + SPLITTER + (validUntilUnixTimestamp / 1000)).toString();
    }

}
