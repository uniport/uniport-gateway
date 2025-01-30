package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CompositeCSPHandler;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSPMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPMiddleware.class);

    private final String name;
    private final CompositeCSPHandler cspHandler;

    public CSPMiddleware(String name, JsonArray cspDirectives, boolean reportOnly) {
        this(name, cspDirectives, reportOnly, CSPMiddlewareFactory.DEFAULT_MERGE_STRATEGY);
    }

    public CSPMiddleware(String name, JsonArray cspDirectives, boolean reportOnly, CSPMergeStrategy mergeStrategy) {
        this.name = name;
        this.cspHandler = CompositeCSPHandler.create(mergeStrategy);
        this.cspHandler.setReportOnly(reportOnly);
        cspDirectives.forEach((directive -> {
            this.addDirective(this.cspHandler, (JsonObject) directive);
        }));
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        ctx.addHeadersEndHandler(v -> this.cspHandler.handleResponse(ctx));
        this.cspHandler.handle(ctx);
    }

    private void addDirective(CSPHandler cspHandler, JsonObject directive) {
        final String name = directive.getString(CSPMiddlewareFactory.CSP_DIRECTIVE_NAME);
        final JsonArray values = directive.getJsonArray(CSPMiddlewareFactory.CSP_DIRECTIVE_VALUES);

        values.forEach(value -> {
            cspHandler.addDirective(name, (String) value);
        });
    }
}
