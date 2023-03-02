package com.inventage.portal.gateway.proxy.middleware.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class ProxyMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddlewareFactory.class);

    private static final String MIDDLEWARE_PROXY = "proxy";

    @Override
    public String provides() {
        return MIDDLEWARE_PROXY;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject serviceConfig) {
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_PROXY);
        return Future.succeededFuture(
                new ProxyMiddleware(vertx,
                        name,
                        serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_PROTOCOL),
                        serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                        serviceConfig.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT)));
    }
}
