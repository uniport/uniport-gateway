package com.inventage.portal.gateway.proxy.middleware.checkRoute;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = CheckRouteMiddlewareOptions.Builder.class)
public final class CheckRouteMiddlewareOptions implements GatewayMiddlewareOptions {

    public static Builder builder() {
        return new Builder();
    }

    private CheckRouteMiddlewareOptions(Builder builder) {
    }

    @Override
    public CheckRouteMiddlewareOptions clone() {
        try {
            return (CheckRouteMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        public CheckRouteMiddlewareOptions build() {
            return new CheckRouteMiddlewareOptions(this);
        }
    }
}
