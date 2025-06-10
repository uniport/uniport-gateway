package com.inventage.portal.gateway.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = GatewayFileProvider.Builder.class)
public abstract class AbstractGatewayFileProvider implements GatewayProvider {

    @Override
    @JsonProperty(StaticConfiguration.PROVIDER_NAME)
    public abstract String getName();

    @JsonProperty(StaticConfiguration.PROVIDER_FILE_FILENAME)
    public abstract String getFilename();

    @JsonProperty(StaticConfiguration.PROVIDER_FILE_DIRECTORY)
    public abstract String getDirectory();

    @JsonProperty(StaticConfiguration.PROVIDER_FILE_WATCH)
    public abstract boolean isWatch();

}
