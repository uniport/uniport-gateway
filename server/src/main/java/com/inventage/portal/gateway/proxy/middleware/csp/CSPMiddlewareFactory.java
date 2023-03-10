package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSPMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_CSP;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final Boolean reportOnly = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_CSP_REPORT_ONLY);
        final JsonArray directives = middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVES);

        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_CSP);
        return Future.succeededFuture(new CSPMiddleware(name, directives, reportOnly));
    }
}
