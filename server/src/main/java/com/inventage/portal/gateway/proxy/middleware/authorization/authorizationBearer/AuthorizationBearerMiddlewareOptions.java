package com.inventage.portal.gateway.proxy.middleware.authorization.authorizationBearer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = AuthorizationBearerMiddlewareOptions.Builder.class)
public final class AuthorizationBearerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(AuthorizationBearerMiddlewareFactory.AUTHORIZATION_BEARER_SESSION_SCOPE)
    private String sessionScope;

    public static Builder builder() {
        return new Builder();
    }

    private AuthorizationBearerMiddlewareOptions(Builder builder) {
        if (builder.sessionScope == null) {
            throw new IllegalArgumentException("session scope is required");
        }

        this.sessionScope = builder.sessionScope;
    }

    public String getSessionScope() {
        return sessionScope;
    }

    @Override
    public AuthorizationBearerMiddlewareOptions clone() {
        try {
            return (AuthorizationBearerMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String sessionScope;

        public Builder withSessionScope(String sessionScope) {
            this.sessionScope = sessionScope;
            return this;
        }

        public AuthorizationBearerMiddlewareOptions build() {
            return new AuthorizationBearerMiddlewareOptions(this);
        }
    }
}
