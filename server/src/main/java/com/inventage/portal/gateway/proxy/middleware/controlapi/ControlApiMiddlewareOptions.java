package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ControlApiMiddlewareOptions.Builder.class)
public final class ControlApiMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_ACTION)
    private String action;

    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_SESSION_RESET_URL)
    private String sessionResetURL;

    public static Builder builder() {
        return new Builder();
    }

    private ControlApiMiddlewareOptions(Builder builder) {
        if (builder.action == null) {
            throw new IllegalArgumentException("action is required");
        }

        this.action = builder.action;
        this.sessionResetURL = builder.sessionResetURL;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private String action;
        private String sessionResetURL;

        public Builder withAction(String action) {
            this.action = action;
            return this;
        }

        public Builder withSessionResetURL(String sessionResetURL) {
            this.sessionResetURL = sessionResetURL;
            return this;
        }

        public ControlApiMiddlewareOptions build() {
            return new ControlApiMiddlewareOptions(this);
        }
    }
}
