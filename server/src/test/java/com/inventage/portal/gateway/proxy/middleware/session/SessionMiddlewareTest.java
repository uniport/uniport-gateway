package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

@ExtendWith(VertxExtension.class)
public class SessionMiddlewareTest {

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
                    return browser.request(GET, "/request2"); })
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
