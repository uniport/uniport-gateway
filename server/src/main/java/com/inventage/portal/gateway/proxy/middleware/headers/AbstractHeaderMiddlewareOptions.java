package com.inventage.portal.gateway.proxy.middleware.headers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import java.util.Map;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = HeaderMiddlewareOptions.Builder.class)
public abstract class AbstractHeaderMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_REQUEST)
    public abstract Map<String, Object> getRequestHeaders();

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_RESPONSE)
    public abstract Map<String, Object> getResponseHeaders();
}
