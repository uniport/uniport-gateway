package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ProxyApplicationFactory implements ApplicationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    @Override
    public String provides() {
        return ProxyApplication.class.getSimpleName();
    }

    @Override
    public Application create(Vertx vertx, JsonObject applicationConfig, JsonArray entrypointConfigs, JsonArray providerConfigs, JsonObject env) {
        LOGGER.info("Created '{}' middleware successfully", "proxy");

        final String entrypointName = applicationConfig.getString(StaticConfiguration.APPLICATION_ENTRYPOINT);

        final int entrypointPort = DynamicConfiguration
            .getObjByKeyWithValue(entrypointConfigs, StaticConfiguration.ENTRYPOINT_NAME, entrypointName)
            .getInteger(StaticConfiguration.ENTRYPOINT_PORT);

        return new ProxyApplication(
            vertx,
            applicationConfig.getString(StaticConfiguration.APPLICATION_NAME),
            entrypointName,
            entrypointPort,
            providerConfigs,
            env);
    }
}
