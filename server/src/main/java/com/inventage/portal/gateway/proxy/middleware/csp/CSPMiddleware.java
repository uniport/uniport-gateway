package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CompositeCSPHandler;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CompositeCSPHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSPMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPMiddleware.class);
    private static final String REPORT_URI = "report-uri";
    private static final String REPORT_TO = "report-to";
    private static final boolean DEFAULT_REPORT_ONLY = false;

    private final String name;
    private final CompositeCSPHandlerImpl cspHandler;

    public CSPMiddleware(String name, JsonArray cspDirectives, Boolean reportOnly) {
        this(name, cspDirectives, reportOnly, CSPMergeStrategy.UNION.toString());
    }

    public CSPMiddleware(String name, JsonArray cspDirectives, Boolean reportOnly, String mergeStrategy) {
        this.name = name;
        this.cspHandler = CompositeCSPHandler.create(mergeStrategy);
        this.cspHandler.setReportOnly((reportOnly == null) ? DEFAULT_REPORT_ONLY : reportOnly);
        cspDirectives.forEach((directive -> {
            this.addDirective(this.cspHandler, (JsonObject) directive);
        }));
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        final Handler<MultiMap> responseHandler = headers -> this.cspHandler.handleResponse(ctx, headers);
        this.addModifier(ctx, responseHandler, Middleware.RESPONSE_HEADERS_MODIFIERS);
        this.cspHandler.handle(ctx);
    }

    private void addDirective(CSPHandler cspHandler, JsonObject directive) {
        final String name = directive.getString(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_NAME);
        final JsonArray values = directive.getJsonArray(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_VALUES);

        values.forEach(value -> {
            cspHandler.addDirective(name, (String) value);

            // TODO (fbuetler) remove, once this issue is fixed: https://github.com/vert-x3/vertx-web/issues/2359
            // io.vertx.ext.web.handler.impl.CSPHandlerImpl.handl in vertx-web:4.3.7 only supports report-uri, which is deprecated.
            // it is suggested to support report-to as well. We enable report-to support by adding this directive to report-uri as well.
            if (name.equals(REPORT_TO)) {
                cspHandler.addDirective(REPORT_URI, (String) value);
            }
        });
    }

    public CSPHandler getCspHandler() {
        return cspHandler;
    }
}
