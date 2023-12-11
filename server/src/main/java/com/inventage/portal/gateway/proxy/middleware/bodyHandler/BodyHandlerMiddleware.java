package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BodyHandlerMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BodyHandlerMiddleware.class);
    private final BodyHandler bodyHandler;

    private final String name;

    public BodyHandlerMiddleware(
        Vertx vertx, String name
    ) {
        this.bodyHandler = BodyHandler.create();
        this.name = name;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        bodyHandler.handle(ctx);
    }
}
