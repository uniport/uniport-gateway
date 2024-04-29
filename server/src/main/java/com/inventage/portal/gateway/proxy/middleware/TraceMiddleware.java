package com.inventage.portal.gateway.proxy.middleware;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages trace creation and termination per middleware
 */
public class TraceMiddleware implements Middleware {

    private final Tracer tracer;

    /**
    */
    public TraceMiddleware() {
        this.tracer = GlobalOpenTelemetry.getTracer(TraceMiddleware.class.getName());
    }

    @Override
    public void handle(RoutingContext ctx) {
        final String spanName = getClass().getName();
        final Span span = tracer.spanBuilder(spanName).startSpan();
        final TraceRoutingContext traceCtx = new TraceRoutingContext(ctx, span);
        try (Scope scope = span.makeCurrent()) {
            handleWithTraceSpan(traceCtx, span);
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, spanName + " middleware failed");
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }

    protected void handleWithTraceSpan(RoutingContext ctx, Span span) {
        ctx.next();
    }

}
