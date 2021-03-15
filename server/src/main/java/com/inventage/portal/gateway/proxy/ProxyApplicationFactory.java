package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 *
 */
public class ProxyApplicationFactory implements ApplicationFactory {

    @Override
    public String provides() {
        return ProxyApplication.class.getSimpleName();
    }

    @Override
    public Application create(JsonObject applicationConfig, JsonObject globalConfig, Vertx vertx) {
        return new ProxyApplication(applicationConfig.getString(Application.NAME),
                applicationConfig.getString(Application.ENTRYPOINT), globalConfig, vertx);
    }
}
