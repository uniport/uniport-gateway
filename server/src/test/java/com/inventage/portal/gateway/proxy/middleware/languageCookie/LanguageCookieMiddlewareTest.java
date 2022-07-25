package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import com.inventage.portal.gateway.TestUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.languageCookie.LanguageCookieMiddleware.IPS_LANGUAGE_COOKIE_NAME;

@ExtendWith(VertxExtension.class)
public class LanguageCookieMiddlewareTest {

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
    public void removeCookieInRequestsTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, IPS_LANGUAGE_COOKIE_NAME + "=de");
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        portalGateway(vertx, host, port)
                .withRoutingContextHolder(routingContext)
                .withLanguageCookieMiddleware()
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions().setHeaders(headers), (incomingResponse) -> {
                    // then
                    Assertions.assertTrue(routingContext.get().request().headers().contains(HttpHeaders.ACCEPT_LANGUAGE),
                            "request should contain accept language");
                    Assertions.assertEquals("de", routingContext.get().request().getHeader(HttpHeaders.ACCEPT_LANGUAGE),
                            "accept-language header should be set to 'de'");
                    Assertions.assertNotNull(routingContext.get().request().getCookie(IPS_LANGUAGE_COOKIE_NAME),
                            "request should contain IPS language cookie.");
                    testCtx.completeNow();
                });
    }

    @Test
    public void noCookieAvailableTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        portalGateway(vertx, host, port)
                .withRoutingContextHolder(routingContext)
                .withLanguageCookieMiddleware()
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions(), (incomingResponse) -> {
                    // then
                    Assertions.assertFalse(routingContext.get().request().headers().contains(IPS_LANGUAGE_COOKIE_NAME),
                            "response should not contain IPS language cookie.");
                    Assertions.assertFalse(routingContext.get().request().headers().contains(HttpHeaders.ACCEPT_LANGUAGE),
                            "response should not contain accept language");
                    testCtx.completeNow();
                });
    }
}
