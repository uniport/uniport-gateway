package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ClaimToHeaderMiddlewareOptions.Builder.class)
public final class ClaimToHeaderMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ClaimToHeaderMiddlewareFactory.CLAIM_TO_HEADER_NAME)
    private String name;

    @JsonProperty(ClaimToHeaderMiddlewareFactory.CLAIM_TO_HEADER_PATH)
    private String path;

    public static Builder builder() {
        return new Builder();
    }

    private ClaimToHeaderMiddlewareOptions(Builder builder) {
        if (builder.name == null) {
            throw new IllegalArgumentException("name is required");
        }
        if (builder.path == null) {
            throw new IllegalArgumentException("path is required");
        }
        this.name = builder.name;
        this.path = builder.path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public ClaimToHeaderMiddlewareOptions clone() {
        try {
            return (ClaimToHeaderMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String name;
        private String path;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        public ClaimToHeaderMiddlewareOptions build() {
            return new ClaimToHeaderMiddlewareOptions(this);
        }
    }
}
