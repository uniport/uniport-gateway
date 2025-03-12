package com.inventage.portal.gateway.proxy.router.additionalRoutes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = AdditionalRoutesMiddlewareOptions.Builder.class)
public abstract class AbstractAdditionalRoutesMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(AdditionalRoutesMiddlewareFactory.ADDITIONAL_ROUTES_PATH)
    public String getPath() {
        return AdditionalRoutesMiddlewareFactory.DEFAULT_ADDITIONAL_ROUTES_PATH;
    }

}
