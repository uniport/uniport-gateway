package com.inventage.portal.gateway.core.middleware.proxy;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface ProxyHandler extends Handler<RoutingContext> {

    static final String MIDDLEWARE_PROXY_SERVER_HOST = "serverHost";
    static final String MIDDLEWARE_PROXY_SERVER_PORT = "serverPort";

    static ProxyHandler create(Vertx vertx, JsonObject middlewareConfig) {
        return new ProxyHandlerImpl(vertx, middlewareConfig.getString(MIDDLEWARE_PROXY_SERVER_HOST),
                middlewareConfig.getInteger(MIDDLEWARE_PROXY_SERVER_PORT));
    }
}
