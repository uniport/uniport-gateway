package com.inventage.portal.gateway.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = Gateway.Builder.class)
public abstract class AbstractGateway {

    @JsonProperty(StaticConfiguration.ENTRYPOINTS)
    public abstract List<GatewayEntrypoint> getEntrypoints();

    @JsonProperty(StaticConfiguration.PROVIDERS)
    public abstract List<GatewayProvider> getProviders();
}
