package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuth2RegistrationMiddlewareOptions extends OAuth2MiddlewareOptions {

    public OAuth2RegistrationMiddlewareOptions() {
    }

    @Override
    public OAuth2RegistrationMiddlewareOptions clone() {
        return (OAuth2RegistrationMiddlewareOptions) super.clone();
    }
}
