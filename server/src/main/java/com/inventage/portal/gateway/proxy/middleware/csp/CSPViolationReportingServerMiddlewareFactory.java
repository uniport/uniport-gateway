package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
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
    public static final String[] CSP_VIOLATION_REPORTING_SERVER_LOG_LEVELS = new String[] {
        "TRACE",
        "DEBUG",
        "INFO",
        "WARN",
        "ERROR"
    };

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
            .requiredProperty(CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL, Schemas.enumSchema((Object[]) CSP_VIOLATION_REPORTING_SERVER_LOG_LEVELS))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return CSPViolationReportingServerMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(final Vertx vertx, final String name, final Router router, final JsonObject middlewareConfig) {
        final String logLevel = middlewareConfig.getString(CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL, DEFAULT_LOG_LEVEL);

        LOGGER.info("Created '{}' middleware successfully", CSP_VIOLATION_REPORTING_SERVER);
        return Future.succeededFuture(new CSPViolationReportingServerMiddleware(name, Level.valueOf(logLevel)));
    }
}
