package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = OAuth2RegistrationMiddlewareOptions.Builder.class)
public final class OAuth2RegistrationMiddlewareOptions extends OAuth2MiddlewareOptions {

    public static Builder builder() {
        return new Builder();
    }

    private OAuth2RegistrationMiddlewareOptions(Builder builder) {
        super(builder);
    }

    @Override
    public OAuth2RegistrationMiddlewareOptions clone() {
        return (OAuth2RegistrationMiddlewareOptions) super.clone();
    }

    @JsonPOJOBuilder
    public static final class Builder extends BaseBuilder<Builder> {
        public OAuth2RegistrationMiddlewareOptions build() {
            return new OAuth2RegistrationMiddlewareOptions(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
