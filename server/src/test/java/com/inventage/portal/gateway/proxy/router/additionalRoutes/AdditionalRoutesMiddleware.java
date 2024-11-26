package com.inventage.portal.gateway.proxy.router.additionalRoutes;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for adding the additional routes to the router
 */
public class AdditionalRoutesMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalRoutesMiddleware.class);

    private final String name;

    public AdditionalRoutesMiddleware(String name) {
        this.name = name;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        ctx.next();
    }
}
