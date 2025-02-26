package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlApiMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_ACTION)
    private String action;

    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_SESSION_RESET_URL)
    private String sessionResetURL;

    public ControlApiMiddlewareOptions() {
    }

    public String getAction() {
        return action;
    }

    public String getSessionResetURL() {
        return sessionResetURL;
    }

    @Override
    public ControlApiMiddlewareOptions clone() {
        try {
            return (ControlApiMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
