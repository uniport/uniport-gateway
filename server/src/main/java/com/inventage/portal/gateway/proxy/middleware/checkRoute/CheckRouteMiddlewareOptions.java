package com.inventage.portal.gateway.proxy.middleware.checkRoute;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckRouteMiddlewareOptions implements GatewayMiddlewareOptions {

    public CheckRouteMiddlewareOptions() {
    }

    @Override
    public CheckRouteMiddlewareOptions clone() {
        try {
            return (CheckRouteMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
