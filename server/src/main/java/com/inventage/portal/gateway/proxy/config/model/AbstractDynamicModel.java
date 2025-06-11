package com.inventage.portal.gateway.proxy.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.DynamicConfiguration;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = DynamicModel.Builder.class)
public abstract class AbstractDynamicModel {

    @JsonProperty(DynamicConfiguration.ROUTERS)
    public abstract List<RouterModel> getRouters();

    @JsonProperty(DynamicConfiguration.MIDDLEWARES)
    public abstract List<MiddlewareModel> getMiddlewares();

    @JsonProperty(DynamicConfiguration.SERVICES)
    public abstract List<ServiceModel> getServices();
}
