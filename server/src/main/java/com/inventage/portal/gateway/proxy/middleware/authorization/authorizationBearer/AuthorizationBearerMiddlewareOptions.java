package com.inventage.portal.gateway.proxy.middleware.authorization.authorizationBearer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationBearerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(AuthorizationBearerMiddlewareFactory.AUTHORIZATION_BEARER_SESSION_SCOPE)
    private String sessionScope;

    public AuthorizationBearerMiddlewareOptions() {
    }

    AuthorizationBearerMiddlewareOptions(String sessionScope) {
        this.sessionScope = sessionScope;
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
}
