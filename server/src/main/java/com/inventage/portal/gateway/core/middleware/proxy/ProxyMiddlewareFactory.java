package com.inventage.portal.gateway.core.middleware.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ProxyMiddlewareFactory implements MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(ProxyMiddlewareFactory.class);

    @Override
    public String provides() {
        return "proxy";
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject serviceConfig) {
        return new ProxyMiddleware(vertx,
                serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                serviceConfig.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT));
    }
}
