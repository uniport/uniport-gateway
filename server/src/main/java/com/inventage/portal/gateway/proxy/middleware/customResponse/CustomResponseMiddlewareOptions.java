package com.inventage.portal.gateway.proxy.middleware.customResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomResponseMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE)
    private Integer statusCode;

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_HEADERS)
    private Map<String, String> headers;

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT)
    private String content;

    public CustomResponseMiddlewareOptions() {
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHedaers() {
        return headers == null ? null : Map.copyOf(headers);
    }

    public String getContent() {
        return content;
    }

    @Override
    public CustomResponseMiddlewareOptions clone() {
        try {
            final CustomResponseMiddlewareOptions options = (CustomResponseMiddlewareOptions) super.clone();
            options.headers = headers == null ? null : Map.copyOf(headers);
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
