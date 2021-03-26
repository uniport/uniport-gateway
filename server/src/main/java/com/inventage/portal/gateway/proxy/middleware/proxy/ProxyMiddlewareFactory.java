package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class ProxyMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddlewareFactory.class);

    @Override
    public String provides() {
        LOGGER.trace("provides");
        return "proxy";
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject serviceConfig) {
        LOGGER.trace("create");
        return this.create(vertx, router, serviceConfig, null);
    }

    public Future<Middleware> create(Vertx vertx, Router router, JsonObject serviceConfig,
            UriMiddleware uriMiddleware) {
        LOGGER.trace("create");
        return Future.succeededFuture(new ProxyMiddleware(vertx,
                serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                serviceConfig.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT), uriMiddleware));
    }
}
