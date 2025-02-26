package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseSessionCookieRemovalMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ResponseSessionCookieRemovalMiddlewareFactory.RESPONSE_SESSION_COOKIE_REMOVAL_NAME)
    private String sessionCookieName;

    public ResponseSessionCookieRemovalMiddlewareOptions() {
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
}
