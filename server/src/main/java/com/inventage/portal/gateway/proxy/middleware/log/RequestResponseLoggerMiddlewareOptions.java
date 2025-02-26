package com.inventage.portal.gateway.proxy.middleware.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestResponseLoggerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_FILTER_REGEX)
    private String filterRegex;

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_CONTENT_TYPES)
    private List<String> contentTypes;

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED)
    private Boolean requestEnabled;

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED)
    private Boolean responseEnabled;

    public RequestResponseLoggerMiddlewareOptions() {
    }

    public String getFilterRegex() {
        return filterRegex;
    }

    public List<String> getContentTypes() {
        return contentTypes == null ? null : List.copyOf(contentTypes);
    }

    public Boolean isRequestEnabled() {
        return requestEnabled;
    }

    public Boolean isResponseEnabled() {
        return responseEnabled;
    }

    @Override
    public RequestResponseLoggerMiddlewareOptions clone() {
        try {
            final RequestResponseLoggerMiddlewareOptions options = (RequestResponseLoggerMiddlewareOptions) super.clone();
            options.contentTypes = contentTypes == null ? null : List.copyOf(contentTypes);
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
