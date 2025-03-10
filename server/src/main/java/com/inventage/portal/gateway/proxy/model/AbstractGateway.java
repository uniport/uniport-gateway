package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = Gateway.Builder.class)
public abstract class AbstractGateway {

    @JsonProperty(DynamicConfiguration.ROUTERS)
    public abstract List<GatewayRouter> getRouters();

    @JsonProperty(DynamicConfiguration.MIDDLEWARES)
    public abstract List<GatewayMiddleware> getMiddlewares();

    @JsonProperty(DynamicConfiguration.SERVICES)
    public abstract List<GatewayService> getServices();
}
