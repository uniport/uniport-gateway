package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = PreventForeignInitiatedAuthMiddlewareOptions.Builder.class)
public final class PreventForeignInitiatedAuthMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(PreventForeignInitiatedAuthMiddlewareFactory.PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT)
    private String redirectURI;

    public static Builder builder() {
        return new Builder();
    }

    private PreventForeignInitiatedAuthMiddlewareOptions(Builder builder) {
        this.redirectURI = builder.redirectURI;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    @Override
    public PreventForeignInitiatedAuthMiddlewareOptions clone() {
        try {
            return (PreventForeignInitiatedAuthMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String redirectURI;

        public Builder withRedirectURI(String redirectURI) {
            this.redirectURI = redirectURI;
            return this;
        }

        public PreventForeignInitiatedAuthMiddlewareOptions build() {
            return new PreventForeignInitiatedAuthMiddlewareOptions(this);
        }
    }
}
