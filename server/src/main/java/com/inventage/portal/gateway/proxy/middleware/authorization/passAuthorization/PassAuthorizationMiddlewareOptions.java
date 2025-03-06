package com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = PassAuthorizationMiddlewareOptions.Builder.class)
public final class PassAuthorizationMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @JsonProperty(PassAuthorizationMiddlewareFactory.PASS_AUTHORIZATION_SESSION_SCOPE)
    private String sessionScope;

    public static Builder builder() {
        return new Builder();
    }

    private PassAuthorizationMiddlewareOptions(Builder builder) {
        super(builder);

        if (builder.sessionScope == null) {
            throw new IllegalArgumentException("session scope is required");
        }
        this.sessionScope = builder.sessionScope;
    }

    public String getSessionScope() {
        return sessionScope;
    }

    @Override
    public PassAuthorizationMiddlewareOptions clone() {
        return (PassAuthorizationMiddlewareOptions) super.clone();
    }

    @JsonPOJOBuilder
    public static final class Builder extends BaseBuilder<Builder> {

        private String sessionScope;

        public Builder withSessionScope(String sessionScope) {
            this.sessionScope = sessionScope;
            return self();
        }

        public PassAuthorizationMiddlewareOptions build() {
            return new PassAuthorizationMiddlewareOptions(this);

        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
