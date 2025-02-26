package com.inventage.portal.gateway.proxy.middleware.debug;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShowSessionContentMiddlewareOptions implements GatewayMiddlewareOptions {

    public ShowSessionContentMiddlewareOptions() {
    }

    @Override
    public ShowSessionContentMiddlewareOptions clone() {
        try {
            return (ShowSessionContentMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
