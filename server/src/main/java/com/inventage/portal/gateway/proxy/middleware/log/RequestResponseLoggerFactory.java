package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestResponseLoggerFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER);
        return Future.succeededFuture(new RequestResponseLogger());
    }

}
