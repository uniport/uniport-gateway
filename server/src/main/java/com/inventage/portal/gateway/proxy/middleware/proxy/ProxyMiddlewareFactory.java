package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ProxyMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddlewareFactory.class);

    @Override
    public String provides() {
        return "proxy";
    }

    @Override
    public Middleware create(Vertx vertx, JsonObject serviceConfig) {
        return this.create(vertx, serviceConfig, null);
    }

    public Middleware create(Vertx vertx, JsonObject serviceConfig, UriMiddleware uriMiddleware) {
        return new ProxyMiddleware(vertx,
                serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                serviceConfig.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT), uriMiddleware);
    }
}
