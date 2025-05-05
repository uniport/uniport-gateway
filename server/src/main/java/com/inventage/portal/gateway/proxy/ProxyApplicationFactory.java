package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyApplicationFactory implements ApplicationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    @Override
    public String provides() {
        return ProxyApplication.class.getSimpleName();
    }

    @Override
    public Application create(Vertx vertx, JsonObject entrypointConfig, JsonArray providerConfigs, JsonObject env) {
        LOGGER.info("Created '{}' middleware successfully", "proxy");

        final String entrypointName = entrypointConfig.getString(StaticConfiguration.ENTRYPOINT_NAME);
        final int entrypointPort = entrypointConfig.getInteger(StaticConfiguration.ENTRYPOINT_PORT);

        return new ProxyApplication(
            vertx,
            "proxy",
            entrypointName,
            entrypointPort,
            providerConfigs,
            env);
    }
}
