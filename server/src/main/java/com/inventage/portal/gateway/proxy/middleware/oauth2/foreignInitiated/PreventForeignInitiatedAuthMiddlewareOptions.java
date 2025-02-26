package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreventForeignInitiatedAuthMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(PreventForeignInitiatedAuthMiddlewareFactory.PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT)
    private String redirectURI;

    public PreventForeignInitiatedAuthMiddlewareOptions() {
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
}
