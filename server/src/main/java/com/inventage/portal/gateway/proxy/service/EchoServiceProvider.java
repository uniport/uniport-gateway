package com.inventage.portal.gateway.proxy.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class EchoServiceProvider implements ServiceProvider {

    @Override
    public String provides() {
        return EchoService.class.getSimpleName();
    }

    @Override
    public Service create(JsonObject serviceConfig, JsonObject globalConfig, Vertx vertx) {
        return new EchoService();
    }
}
