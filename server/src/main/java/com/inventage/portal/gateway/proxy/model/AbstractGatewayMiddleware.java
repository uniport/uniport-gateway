package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.model.deserialize.GatewayMiddlewareJsonDeserializer;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(using = GatewayMiddlewareJsonDeserializer.class, builder = GatewayMiddleware.Builder.class)
public abstract class AbstractGatewayMiddleware {

    @JsonProperty(DynamicConfiguration.MIDDLEWARE_NAME)
    public abstract String getName();

    @JsonProperty(DynamicConfiguration.MIDDLEWARE_TYPE)
    public abstract String getType();

    @Nullable
    @JsonProperty(DynamicConfiguration.MIDDLEWARE_OPTIONS)
    public abstract GatewayMiddlewareOptions getOptions();
}
