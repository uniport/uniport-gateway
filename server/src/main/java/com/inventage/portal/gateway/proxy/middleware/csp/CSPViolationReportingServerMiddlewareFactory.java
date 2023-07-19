package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSPViolationReportingServerMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPViolationReportingServerMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_CSP_VIOLATION_REPORTING_SERVER;
    }

    @Override
    public Future<Middleware> create(final Vertx vertx, final String name, final Router router, final JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_CSP);
        return Future.succeededFuture(new CSPViolationReportingServerMiddleware(name));
    }
}
