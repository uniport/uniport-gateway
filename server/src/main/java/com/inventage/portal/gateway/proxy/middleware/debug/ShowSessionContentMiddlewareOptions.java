package com.inventage.portal.gateway.proxy.middleware.debug;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ShowSessionContentMiddlewareOptions.Builder.class)
public final class ShowSessionContentMiddlewareOptions implements GatewayMiddlewareOptions {

    public static Builder builder() {
        return new Builder();
    }

    private ShowSessionContentMiddlewareOptions(Builder builder) {
    }

    @Override
    public ShowSessionContentMiddlewareOptions clone() {
        try {
            return (ShowSessionContentMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        public ShowSessionContentMiddlewareOptions build() {
            return new ShowSessionContentMiddlewareOptions(this);
        }
    }
}
