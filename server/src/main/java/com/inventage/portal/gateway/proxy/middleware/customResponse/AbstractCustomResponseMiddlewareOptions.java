package com.inventage.portal.gateway.proxy.middleware.customResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.Map;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = CustomResponseMiddlewareOptions.Builder.class)
public abstract class AbstractCustomResponseMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CustomResponseMiddlewareFactory.STATUS_CODE)
    public abstract int getStatusCode();

    @JsonProperty(CustomResponseMiddlewareFactory.HEADERS)
    public abstract Map<String, String> getHeaders();

    @JsonProperty(CustomResponseMiddlewareFactory.CONTENT)
    public abstract String getContent();
}
