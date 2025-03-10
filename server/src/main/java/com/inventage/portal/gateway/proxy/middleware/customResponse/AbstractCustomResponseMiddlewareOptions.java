package com.inventage.portal.gateway.proxy.middleware.customResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import java.util.Map;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = CustomResponseMiddlewareOptions.Builder.class)
public abstract class AbstractCustomResponseMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE)
    public abstract int getStatusCode();

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_HEADERS)
    public abstract Map<String, String> getHeaders();

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT)
    public abstract String getContent();
}
