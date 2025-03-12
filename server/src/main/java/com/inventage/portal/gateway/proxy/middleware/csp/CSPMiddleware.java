package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CompositeCSPHandler;
import io.opentelemetry.api.trace.Span;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSPMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPMiddleware.class);

    private final String name;
    private final CompositeCSPHandler cspHandler;

    public CSPMiddleware(String name, List<DirectiveOptions> directives, boolean reportOnly, CSPMergeStrategy mergeStrategy) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(directives, "directives must not be null");
        Objects.requireNonNull(mergeStrategy, "mergeStrategy must not be null");

        this.name = name;
        this.cspHandler = CompositeCSPHandler.create(mergeStrategy);
        this.cspHandler.setReportOnly(reportOnly);
        directives.forEach((directive -> {
            this.addDirective(this.cspHandler, directive);
        }));
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        ctx.addHeadersEndHandler(v -> this.cspHandler.handleResponse(ctx));
        this.cspHandler.handle(ctx);
    }

    private void addDirective(CSPHandler cspHandler, DirectiveOptions directive) {
        directive.getValues().forEach(value -> {
            cspHandler.addDirective(directive.getName(), value);
        });
    }
}
