package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import java.util.List;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = GatewayRouter.Builder.class)
public abstract class AbstractGatewayRouter {

    @JsonProperty(DynamicConfiguration.ROUTER_NAME)
    public abstract String getName();

    @JsonProperty(DynamicConfiguration.ROUTER_RULE)
    public abstract String getRule();

    @Default
    @JsonProperty(DynamicConfiguration.ROUTER_PRIORITY)
    public int getPriority() {
        return 0;
    }

    @JsonProperty(DynamicConfiguration.ROUTER_ENTRYPOINTS)
    public abstract List<String> getEntrypoints();

    @JsonProperty(DynamicConfiguration.ROUTER_MIDDLEWARES)
    public abstract List<String> getMiddlewares();

    @JsonProperty(DynamicConfiguration.ROUTER_SERVICE)
    public abstract String getService();
}
