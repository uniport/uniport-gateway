package com.inventage.portal.gateway.proxy.middleware.customResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = CustomResponseMiddlewareOptions.Builder.class)
public final class CustomResponseMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE)
    private Integer statusCode;

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_HEADERS)
    private Map<String, String> headers;

    @JsonProperty(CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT)
    private String content;

    public static Builder builder() {
        return new Builder();
    }

    private CustomResponseMiddlewareOptions(Builder builder) {
        if (builder.statusCode == null) {
            throw new IllegalArgumentException("status code is required");
        }
        if (builder.content == null) {
            throw new IllegalArgumentException("content is required");
        }

        this.statusCode = builder.statusCode;
        this.headers = builder.headers;
        this.content = builder.content;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private Integer statusCode;
        private Map<String, String> headers;
        private String content;

        public Builder withStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder withHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder withContent(String content) {
            this.content = content;
            return this;
        }

        public CustomResponseMiddlewareOptions build() {
            return new CustomResponseMiddlewareOptions(this);
        }
    }
}
