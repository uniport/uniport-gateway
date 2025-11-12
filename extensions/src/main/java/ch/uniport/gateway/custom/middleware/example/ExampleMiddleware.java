package ch.uniport.gateway.custom.middleware.example;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for adding the additional routes to the router
 */
public class ExampleMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleMiddleware.class);

    private final String name;
    private final String headerName;
    private final String headerValue;

    public ExampleMiddleware(String name, String headerName, String headerValue) {
        this.name = name;
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        ctx.request()
            .headers()
            .set(headerName, headerValue);

        ctx.next();
    }
}
