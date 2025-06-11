package com.inventage.portal.gateway.proxy.middleware.customResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import java.util.Map;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CustomResponseMiddlewareOptions.Builder.class)
public abstract class AbstractCustomResponseMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(CustomResponseMiddlewareFactory.STATUS_CODE)
    public abstract int getStatusCode();

    @JsonProperty(CustomResponseMiddlewareFactory.HEADERS)
    public abstract Map<String, String> getHeaders();

    @JsonProperty(CustomResponseMiddlewareFactory.CONTENT)
    public abstract String getContent();
}
