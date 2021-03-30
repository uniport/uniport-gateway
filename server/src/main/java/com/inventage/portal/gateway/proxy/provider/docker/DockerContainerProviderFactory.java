package com.inventage.portal.gateway.proxy.provider.docker;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.ProviderFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class DockerContainerProviderFactory implements ProviderFactory {

    private static final String defaultTemplateRule = "Host('${name}')";

    @Override
    public String provides() {
        LOGGER.trace("provides");
        return StaticConfiguration.PROVIDER_DOCKER;
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig) {
        LOGGER.trace("create");
        String endpoint = providerConfig.getString(StaticConfiguration.PROVIDER_DOCKER_ENDPOINT,
                "unix:///var/run/docker.sock");
        String defaultRule = providerConfig
                .getString(StaticConfiguration.PROVIDER_DOCKER_DEFAULT_RULE, defaultTemplateRule);
        Boolean watch = providerConfig.getBoolean(StaticConfiguration.PROVIDER_FILE_WATCH, false);
        return new DockerContainerProvider(vertx, configurationAddress, endpoint, defaultRule,
                watch);
    }

}
