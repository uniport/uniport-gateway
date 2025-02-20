package com.inventage.portal.gateway.proxy.middleware.csp;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CompositeCSPHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link CSPMiddleware}.
 */
public class CSPMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String CSP = "csp";
    public static final String CSP_DIRECTIVES = "policyDirectives";
    public static final String CSP_DIRECTIVE_NAME = "directive";
    public static final String CSP_DIRECTIVE_VALUES = "values";
    public static final String CSP_REPORT_ONLY = "reportOnly";
    public static final String CSP_MERGE_STRATEGY = "mergeStrategy";
    public static final String CSP_MERGE_STRATEGY_UNION = "UNION";
    public static final String CSP_MERGE_STRATEGY_EXTERNAL = "EXTERNAL";
    public static final String CSP_MERGE_STRATEGY_INTERNAL = "INTERNAL";

    public static final String[] CSP_MERGE_STRATEGIES = new String[] {
        CSP_MERGE_STRATEGY_UNION,
        CSP_MERGE_STRATEGY_EXTERNAL,
        CSP_MERGE_STRATEGY_INTERNAL
    };

    // defaults
    public static final boolean DEFAULT_REPORT_ONLY = false;
    public static final CSPMergeStrategy DEFAULT_MERGE_STRATEGY = CSPMergeStrategy.UNION;

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPMiddlewareFactory.class);

    @Override
    public String provides() {
        return CSP;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(CSP_DIRECTIVES, Schemas.arraySchema()
                .items(Schemas.objectSchema()
                    .requiredProperty(CSP_DIRECTIVE_NAME, Schemas.stringSchema()
                        .with(Keywords.minLength(1)))
                    .requiredProperty(CSP_DIRECTIVE_VALUES, Schemas.arraySchema()
                        .items(Schemas.stringSchema()
                            .with(Keywords.minLength(1))))
                    .allowAdditionalProperties(false)))
            .optionalProperty(CSP_REPORT_ONLY, Schemas.booleanSchema()
                .defaultValue(DEFAULT_REPORT_ONLY))
            .optionalProperty(CSP_MERGE_STRATEGY, Schemas.enumSchema((Object[]) CSP_MERGE_STRATEGIES)
                .defaultValue(DEFAULT_MERGE_STRATEGY))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonArray directives = options.getJsonArray(CSP_DIRECTIVES);
        if (directives == null) {
            return Future.failedFuture(new IllegalStateException("Directive is not defined as JsonObject"));
        }

        boolean hasReportToOrUriDirective = false;
        for (Object d : directives.getList()) {
            if (!(d instanceof JsonObject)) {
                return Future.failedFuture(new IllegalStateException("Directive must be a JsonObject"));
            }
            final JsonObject directive = (JsonObject) d;

            final String name = directive.getString(CSP_DIRECTIVE_NAME);
            if (name == null) {
                return Future.failedFuture(new IllegalStateException("Directive name is not defined"));
            }
            if (name.equals(CompositeCSPHandler.REPORT_URI) || name.equals(CompositeCSPHandler.REPORT_TO)) {
                hasReportToOrUriDirective = true;
            }
        }

        final Boolean reportOnly = options.getBoolean(CSP_REPORT_ONLY, CSPMiddlewareFactory.DEFAULT_REPORT_ONLY);
        if (reportOnly && !hasReportToOrUriDirective) {
            return Future.failedFuture("Reporting enabled, but no report-uri or report-to is configured");
        }

        logDefaultIfNotConfigured(LOGGER, options, CSP_REPORT_ONLY, DEFAULT_REPORT_ONLY);
        logDefaultIfNotConfigured(LOGGER, options, CSP_MERGE_STRATEGY, DEFAULT_MERGE_STRATEGY);

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final Boolean reportOnly = middlewareConfig.getBoolean(CSP_REPORT_ONLY, DEFAULT_REPORT_ONLY);
        final JsonArray directives = middlewareConfig.getJsonArray(CSP_DIRECTIVES);
        final String mergeStrategy = middlewareConfig.getString(CSP_MERGE_STRATEGY, DEFAULT_MERGE_STRATEGY.toString());

        LOGGER.info("Created '{}' middleware successfully", CSP);
        return Future.succeededFuture(new CSPMiddleware(name, directives, reportOnly.booleanValue(), CSPMergeStrategy.valueOf(mergeStrategy)));
    }
}
