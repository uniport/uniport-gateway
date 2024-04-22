package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.DetectionCookieValue.MAX_RETRIES;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.DetectionCookieValue.SPLITTER;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_DETECTION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ReplacedSessionCookieDetectionMiddlewareTest {

    @Test
    public void shouldRedirect(Vertx vertx, VertxTestContext testCtx) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_SESSION_COOKIE_NAME + "=a-session-id-which-has-been-replaced");
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + "=" + new DetectionCookieValue().toString());
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
    public void shouldNotRedirect_when_session_cookie_is_missing(Vertx vertx, VertxTestContext testCtx) {
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + "=" + new DetectionCookieValue().toString());
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
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + "=" + new DetectionCookieValue((MAX_RETRIES - 1) + SPLITTER + 1000).toString());
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
        headers.add(HttpHeaders.COOKIE, DEFAULT_DETECTION_COOKIE_NAME + "=" + new DetectionCookieValue(MAX_RETRIES + SPLITTER + System.currentTimeMillis()).toString());
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

}
