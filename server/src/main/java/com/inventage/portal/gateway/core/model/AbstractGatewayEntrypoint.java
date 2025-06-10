package com.inventage.portal.gateway.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.model.GatewayMiddleware;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = GatewayEntrypoint.Builder.class)
public abstract class AbstractGatewayEntrypoint {

    @JsonProperty(StaticConfiguration.ENTRYPOINT_NAME)
    public abstract String getName();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_PORT)
    public abstract int getPort();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_MIDDLEWARES)
    public abstract List<GatewayMiddleware> getMiddlewares();

}
