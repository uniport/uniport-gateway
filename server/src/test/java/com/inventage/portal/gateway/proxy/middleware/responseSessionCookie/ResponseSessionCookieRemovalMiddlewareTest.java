package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;

@ExtendWith(VertxExtension.class)
public class ResponseSessionCookieRemovalMiddlewareTest {

    @Test
    public void shouldRemoveSessionCookieInResponse(Vertx vertx, VertxTestContext testCtx) {
        MiddlewareServer gateway = portalGateway(vertx)
                .withResponseSessionCookieRemovalMiddleware()
                .withSessionMiddleware()
                .withMiddleware(addingSignal())
                .build().start();

        // when
        gateway.incomingRequest(GET, "/", testCtx, (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                    .hasStatusCode(200)
                    .hasNotSetCookieForSession();
            testCtx.completeNow();
        });
    }

    @Test
    public void shouldContainSessionCookieInResponse(Vertx vertx, VertxTestContext testCtx) {
        MiddlewareServer gateway = portalGateway(vertx)
                .withResponseSessionCookieRemovalMiddleware()
                .withSessionMiddleware()
                .build().start();

        // when
        gateway.incomingRequest(GET, "/", testCtx, (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                    .hasStatusCode(200)
                    .hasSetCookieForSession(null);
            testCtx.completeNow();
        });
    }

    private Handler<RoutingContext> addingSignal() {
        return ctx -> {
            ResponseSessionCookieRemovalMiddleware.addSignal(ctx);
            ctx.response().end();
        };
    }
}
