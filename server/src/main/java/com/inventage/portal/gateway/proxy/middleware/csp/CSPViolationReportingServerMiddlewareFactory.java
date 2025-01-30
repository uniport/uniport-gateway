package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Factory for {@link CSPViolationReportingServerMiddleware}.
 */
public class CSPViolationReportingServerMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String CSP_VIOLATION_REPORTING_SERVER = "cspViolationReportingServer";
    public static final String CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL = "logLevel";
    public static final List<String> CSP_VIOLATION_REPORTING_SERVER_LOG_LEVELS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    // defaults
    public static final String DEFAULT_LOG_LEVEL = "WARN";

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPViolationReportingServerMiddlewareFactory.class);

    @Override
    public String provides() {
        return CSP_VIOLATION_REPORTING_SERVER;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL, Schemas.stringSchema()
                .withKeyword(KEYWORD_ENUM, JsonArray.of(CSP_VIOLATION_REPORTING_SERVER_LOG_LEVELS.toArray())))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final String logLevel = options.getString(CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL);
        if (logLevel != null && !CSP_VIOLATION_REPORTING_SERVER_LOG_LEVELS.contains(logLevel)) {
            return Future.failedFuture(String.format("%s: value '%s' not allowed, must be one on %s",
                CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL, logLevel, CSP_VIOLATION_REPORTING_SERVER_LOG_LEVELS));
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(final Vertx vertx, final String name, final Router router, final JsonObject middlewareConfig) {
        final String logLevel = middlewareConfig.getString(CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL, DEFAULT_LOG_LEVEL);

        LOGGER.info("Created '{}' middleware successfully", CSP_VIOLATION_REPORTING_SERVER);
        return Future.succeededFuture(new CSPViolationReportingServerMiddleware(name, Level.valueOf(logLevel)));
    }
}
