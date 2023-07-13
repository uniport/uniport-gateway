package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.openTelemetry.OpenTelemetryMiddleware.CONTEXTUAL_DATA_TRACE_ID;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class OpenTelemetryMiddlewareTest {

    @Disabled
    @Test
    public void test(Vertx vertx, VertxTestContext testCtx) {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withMiddleware(new OpenTelemetryMiddleware("openTelemetry"))
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", testCtx, (outgoingResponse) -> {
            // then
            Assertions.assertNotNull(ContextualData.get(CONTEXTUAL_DATA_TRACE_ID),
                "contextual data should contain 'traceId'");
            testCtx.completeNow();
        });

        // when
        // then
    }
}
