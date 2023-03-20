package com.inventage.portal.gateway.proxy.middleware.session;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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

}
