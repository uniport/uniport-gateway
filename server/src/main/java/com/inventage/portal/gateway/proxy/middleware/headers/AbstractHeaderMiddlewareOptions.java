package com.inventage.portal.gateway.proxy.middleware.headers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import java.util.Map;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = HeaderMiddlewareOptions.Builder.class)
public abstract class AbstractHeaderMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_REQUEST)
    public abstract Map<String, Object> getRequestHeaders();

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_RESPONSE)
    public abstract Map<String, Object> getResponseHeaders();
}
