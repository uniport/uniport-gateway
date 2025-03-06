package com.inventage.portal.gateway.proxy.middleware.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = RequestResponseLoggerMiddlewareOptions.Builder.class)
public final class RequestResponseLoggerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_FILTER_REGEX)
    private String filterRegex;

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_CONTENT_TYPES)
    private List<String> contentTypes;

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED)
    private Boolean requestEnabled;

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED)
    private Boolean responseEnabled;

    public static Builder builder() {
        return new Builder();
    }

    private RequestResponseLoggerMiddlewareOptions(Builder builder) {
        this.filterRegex = builder.filterRegex;
        this.contentTypes = builder.contentTypes;
        this.requestEnabled = builder.requestEnabled;
        this.responseEnabled = builder.responseEnabled;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private String filterRegex;
        private List<String> contentTypes;
        private Boolean requestEnabled;
        private Boolean responseEnabled;

        public Builder withFilterRegex(String filterRegex) {
            this.filterRegex = filterRegex;
            return this;
        }

        public Builder withContentTypes(List<String> contentTypes) {
            this.contentTypes = contentTypes;
            return this;
        }

        public Builder withRequestEnabled(Boolean requestEnabled) {
            this.requestEnabled = requestEnabled;
            return this;
        }

        public Builder withResponseEnabled(Boolean responseEnabled) {
            this.responseEnabled = responseEnabled;
            return this;
        }

        public RequestResponseLoggerMiddlewareOptions build() {
            return new RequestResponseLoggerMiddlewareOptions(this);
        }
    }
}
