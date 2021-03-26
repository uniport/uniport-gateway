package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ProxyApplicationFactory implements ApplicationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    @Override
    public String provides() {
        LOGGER.trace("provides");
        return ProxyApplication.class.getSimpleName();
    }

    @Override
    public Application create(JsonObject applicationConfig, JsonObject globalConfig, Vertx vertx) {
        LOGGER.trace("create");
        return new ProxyApplication(applicationConfig.getString(Application.NAME),
                applicationConfig.getString(Application.ENTRYPOINT), globalConfig, vertx);
    }
}
