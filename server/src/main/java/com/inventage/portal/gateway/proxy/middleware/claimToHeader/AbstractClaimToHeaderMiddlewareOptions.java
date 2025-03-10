package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = ClaimToHeaderMiddlewareOptions.Builder.class)
public abstract class AbstractClaimToHeaderMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ClaimToHeaderMiddlewareFactory.CLAIM_TO_HEADER_NAME)
    public abstract String getName();

    @JsonProperty(ClaimToHeaderMiddlewareFactory.CLAIM_TO_HEADER_PATH)
    public abstract String getPath();
}