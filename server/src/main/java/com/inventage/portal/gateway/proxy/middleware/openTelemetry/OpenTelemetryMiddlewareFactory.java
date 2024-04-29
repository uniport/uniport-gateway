package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class OpenTelemetryMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OPEN_TELEMETRY;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' of type '{}' middleware successfully", name,
            DynamicConfiguration.MIDDLEWARE_OPEN_TELEMETRY);
        return Future.succeededFuture(new OpenTelemetryMiddleware(name));
    }
}
