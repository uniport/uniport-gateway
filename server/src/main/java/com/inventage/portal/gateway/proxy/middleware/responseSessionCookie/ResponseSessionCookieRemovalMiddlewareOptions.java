package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ResponseSessionCookieRemovalMiddlewareOptions.Builder.class)
public final class ResponseSessionCookieRemovalMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ResponseSessionCookieRemovalMiddlewareFactory.RESPONSE_SESSION_COOKIE_REMOVAL_NAME)
    private String sessionCookieName;

    public static Builder builder() {
        return new Builder();
    }

    private ResponseSessionCookieRemovalMiddlewareOptions(Builder builder) {
        this.sessionCookieName = builder.sessionCookieName;
    }

    public String getSessionCookieName() {
        return sessionCookieName;
    }

    @Override
    public ResponseSessionCookieRemovalMiddlewareOptions clone() {
        try {
            return (ResponseSessionCookieRemovalMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String sessionCookieName;

        public Builder witSessionCookieName(String sessionCookieName) {
            this.sessionCookieName = sessionCookieName;
            return this;
        }

        public ResponseSessionCookieRemovalMiddlewareOptions build() {
            return new ResponseSessionCookieRemovalMiddlewareOptions(this);
        }
    }
}
