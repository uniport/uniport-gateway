package com.inventage.portal.gateway.core.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareModel;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = EntrypointModel.Builder.class)
public abstract class AbstractEntrypointModel {

    @JsonProperty(StaticConfiguration.ENTRYPOINT_NAME)
    public abstract String getName();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_PORT)
    public abstract int getPort();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_MIDDLEWARES)
    public abstract List<MiddlewareModel> getMiddlewares();

}
