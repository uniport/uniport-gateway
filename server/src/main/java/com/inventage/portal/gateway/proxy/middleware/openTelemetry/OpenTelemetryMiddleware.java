package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.log.ContextualDataAdapter;
import com.inventage.portal.gateway.proxy.middleware.log.SessionAdapter;
import io.opentelemetry.api.trace.Span;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This middleware should be one of the first middlewares in the request chain.
 * Adds the following items to the logging contextual data.:
 * - OpenTelemetry trace id (with key "traceId")
 * - Vert.x session id (with key "sessionId")
 * And adds the "X-Uniport-Trace-Id" HTTP header to the outgoing response.
 *
 * logging contextual data: https://reactiverse.io/reactiverse-contextual-logging/
 */
public class OpenTelemetryMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryMiddleware.class);

    public static final String HTTP_HEADER_REQUEST_ID = "X-Uniport-Trace-Id";

    public static final String CONTEXTUAL_DATA_TRACE_ID = "traceId";

    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";

    private final String name;

    public OpenTelemetryMiddleware(String name) {
        this.name = name;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        final String openTelemetryTraceId = getOpenTelemetryTraceId();

        ContextualDataAdapter.put(CONTEXTUAL_DATA_TRACE_ID, openTelemetryTraceId);
        ContextualDataAdapter.put(CONTEXTUAL_DATA_SESSION_ID, getDisplaySessionId(ctx));

        ctx.addHeadersEndHandler(
            v -> ctx.response().putHeader(HTTP_HEADER_REQUEST_ID, openTelemetryTraceId));

        ctx.next();
    }

    protected String getOpenTelemetryTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }

    private String getDisplaySessionId(RoutingContext ctx) {
        return SessionAdapter.displaySessionId(ctx);
    }
}
