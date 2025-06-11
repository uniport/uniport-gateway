package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ClaimToHeaderMiddlewareOptions.Builder.class)
public abstract class AbstractClaimToHeaderMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(ClaimToHeaderMiddlewareFactory.NAME)
    public abstract String getName();

    @JsonProperty(ClaimToHeaderMiddlewareFactory.PATH)
    public abstract String getPath();
}