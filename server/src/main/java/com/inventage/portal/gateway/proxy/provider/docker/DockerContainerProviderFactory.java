package com.inventage.portal.gateway.proxy.provider.docker;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.ProviderFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class DockerContainerProviderFactory implements ProviderFactory {

    @Override
    public String provides() {
        LOGGER.trace("provides");
        return StaticConfiguration.PROVIDER_DOCKER;
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig) {
        LOGGER.trace("create");
        return new DockerContainerProvider(vertx, configurationAddress);
    }

}
