package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BodyHandlerMiddlewareOptions implements GatewayMiddlewareOptions {

    public BodyHandlerMiddlewareOptions() {
    }

    @Override
    public BodyHandlerMiddlewareOptions clone() {
        try {
            return (BodyHandlerMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
