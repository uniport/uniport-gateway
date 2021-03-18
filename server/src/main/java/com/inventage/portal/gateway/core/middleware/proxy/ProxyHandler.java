package com.inventage.portal.gateway.core.middleware.proxy;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface ProxyHandler extends Handler<RoutingContext> {

    static ProxyHandler create(Vertx vertx, JsonObject serviceConfig) {
        return new ProxyHandlerImpl(vertx,
                serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                serviceConfig.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT));
    }
}
