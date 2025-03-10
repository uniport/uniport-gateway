package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = BearerOnlyMiddlewareOptions.Builder.class)
public abstract class AbstractBearerOnlyMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @Default
    @JsonProperty(BearerOnlyMiddlewareFactory.BEARER_ONLY_OPTIONAL)
    public boolean isOptional() {
        return BearerOnlyMiddlewareFactory.DEFAULT_OPTIONAL;
    }
}
