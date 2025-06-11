package com.inventage.portal.gateway.core.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = StaticModel.Builder.class)
public abstract class AbstractStaticModel {

    @JsonProperty(StaticConfiguration.ENTRYPOINTS)
    public abstract List<EntrypointModel> getEntrypoints();

    @JsonProperty(StaticConfiguration.PROVIDERS)
    public abstract List<ProviderModel> getProviders();
}
