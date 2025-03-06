package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = OpenTelemetryMiddlewareOptions.Builder.class)
public final class OpenTelemetryMiddlewareOptions implements GatewayMiddlewareOptions {

    public static Builder builder() {
        return new Builder();
    }

    private OpenTelemetryMiddlewareOptions(Builder builder) {
    }

    @Override
    public OpenTelemetryMiddlewareOptions clone() {
        try {
            return (OpenTelemetryMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        public OpenTelemetryMiddlewareOptions build() {
            return new OpenTelemetryMiddlewareOptions(this);
        }
    }
}
