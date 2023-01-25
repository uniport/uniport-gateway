package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;

@ExtendWith(VertxExtension.class)
public class SessionMiddlewareTest {

    @Test
    public void shouldHaveSessionCookieInResponse(Vertx vertx, VertxTestContext testCtx) {
        MiddlewareServer gateway = portalGateway(vertx)
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
}
