package com.inventage.portal.gateway.proxy.service;

import com.inventage.portal.gateway.core.config.ConfigAdapter;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.httpproxy.HttpProxy;

public class ServiceJsonFileProvider implements ServiceProvider {

    public static final String NAME = "name";
    public static final String SERVER_HOST = "serverHost"; // can contain env variables
    public static final String SERVER_PORT = "serverPort";

    @Override
    public String provides() {
        return getClass().getName();
    }

    @Override
    public Service create(JsonObject serviceConfig, JsonObject globalConfig, Vertx vertx) {
        return new ServiceJsonFile(serviceConfig.getString(NAME),
                ConfigAdapter.replaceEnvVariables(globalConfig,
                        serviceConfig.getString(SERVER_HOST)),
                serviceConfig.getInteger(SERVER_PORT),
                HttpProxy.reverseProxy2(vertx.createHttpClient()));
    }
}
