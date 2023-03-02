package com.inventage.portal.gateway.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import com.inventage.portal.gateway.core.config.StaticConfiguration;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ProxyApplicationFactory implements ApplicationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    @Override
    public String provides() {
        return ProxyApplication.class.getSimpleName();
    }

    @Override
    public Application create(JsonObject applicationConfig, JsonObject globalConfig, Vertx vertx) {
        LOGGER.info("Created '{}' middleware successfully", "proxy");
        return new ProxyApplication(applicationConfig.getString(StaticConfiguration.APPLICATION_NAME),
                applicationConfig.getString(StaticConfiguration.APPLICATION_ENTRYPOINT), globalConfig, vertx);
    }
}
