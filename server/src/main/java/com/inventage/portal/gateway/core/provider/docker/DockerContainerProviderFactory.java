package com.inventage.portal.gateway.core.provider.docker;

import com.inventage.portal.gateway.core.provider.Provider;
import com.inventage.portal.gateway.core.provider.ProviderFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class DockerContainerProviderFactory implements ProviderFactory {
    public static final String PROVIDER_NAME = "docker";

    @Override
    public String provides() {
        return "docker";
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig) {
        return new DockerContainerProvider(vertx, configurationAddress);
    }

}
