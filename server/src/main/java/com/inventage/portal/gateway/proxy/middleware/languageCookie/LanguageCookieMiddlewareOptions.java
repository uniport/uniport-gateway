package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LanguageCookieMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME)
    private String cookieName;

    public LanguageCookieMiddlewareOptions() {
    }

    public String getCookieName() {
        return cookieName;
    }

    @Override
    public LanguageCookieMiddlewareOptions clone() {
        try {
            return (LanguageCookieMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
