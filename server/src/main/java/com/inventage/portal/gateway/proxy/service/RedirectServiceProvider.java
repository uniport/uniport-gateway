package com.inventage.portal.gateway.proxy.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class RedirectServiceProvider implements ServiceProvider {

    @Override
    public String provides() {
        return getClass().getName();
    }

    @Override
    public Service create(JsonObject serviceConfig, JsonObject globalConfig, Vertx vertx) {
        return new RedirectService(serviceConfig);
    }
}
