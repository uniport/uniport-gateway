package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenTelemetryMiddlewareOptions implements GatewayMiddlewareOptions {

    public OpenTelemetryMiddlewareOptions() {
    }

    @Override
    public OpenTelemetryMiddlewareOptions clone() {
        try {
            return (OpenTelemetryMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
