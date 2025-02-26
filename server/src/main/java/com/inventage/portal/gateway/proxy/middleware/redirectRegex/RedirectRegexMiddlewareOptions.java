package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedirectRegexMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REGEX)
    private String regex;

    @JsonProperty(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REPLACEMENT)
    private String replacement;

    public RedirectRegexMiddlewareOptions() {
    }

    public String getRegex() {
        return regex;
    }

    public String getReplacement() {
        return replacement;
    }

    @Override
    public RedirectRegexMiddlewareOptions clone() {
        try {
            return (RedirectRegexMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
