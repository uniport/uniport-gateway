package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ClaimToHeaderMiddlewareOptions.Builder.class)
public abstract class AbstractClaimToHeaderMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ClaimToHeaderMiddlewareFactory.NAME)
    public abstract String getName();

    @JsonProperty(ClaimToHeaderMiddlewareFactory.PATH)
    public abstract String getPath();
}