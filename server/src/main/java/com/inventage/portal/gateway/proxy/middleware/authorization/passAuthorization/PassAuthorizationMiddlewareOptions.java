package com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PassAuthorizationMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @JsonProperty(PassAuthorizationMiddlewareFactory.PASS_AUTHORIZATION_SESSION_SCOPE)
    private String sessionScope;

    public PassAuthorizationMiddlewareOptions() {
    }

    public String getSessionScope() {
        return sessionScope;
    }

    @Override
    public PassAuthorizationMiddlewareOptions clone() {
        return (PassAuthorizationMiddlewareOptions) super.clone();
    }
}
