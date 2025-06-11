package com.inventage.portal.gateway.core.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.provider.docker.DockerContainerProviderFactory;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = DockerProviderModel.Builder.class)
public abstract class AbstractDockerProviderModel implements ProviderModel {

    @Override
    @JsonProperty(StaticConfiguration.PROVIDER_NAME)
    public abstract String getName();

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_ENDPOINT)
    public String getEndpoint() {
        return DockerContainerProviderFactory.DEFAULT_ENDPOINT;
    }

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_EXPOSED_BY_DEFAULT)
    public boolean isExposedByDefault() {
        return DockerContainerProviderFactory.DEFAULT_EXPOSED_BY_DEFAULT;
    }

    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_NETWORK)
    public abstract String getNetwork();

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_DEFAULT_RULE)
    public String getDefaultRule() {
        return DockerContainerProviderFactory.DEFAULT_RULE_TEMPLATE;
    }

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_WATCH)
    public boolean isWatch() {
        return DockerContainerProviderFactory.DEFAULT_WATCH;
    }

}
