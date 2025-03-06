package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = BodyHandlerMiddlewareOptions.Builder.class)
public final class BodyHandlerMiddlewareOptions implements GatewayMiddlewareOptions {

    public static Builder builder() {
        return new Builder();
    }

    private BodyHandlerMiddlewareOptions(Builder builder) {
    }

    @Override
    public BodyHandlerMiddlewareOptions clone() {
        try {
            return (BodyHandlerMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        public BodyHandlerMiddlewareOptions build() {
            return new BodyHandlerMiddlewareOptions(this);
        }
    }
}
