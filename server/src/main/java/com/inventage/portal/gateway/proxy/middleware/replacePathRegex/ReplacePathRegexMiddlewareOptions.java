package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplacePathRegexMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REGEX)
    private String regex;

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REPLACEMENT)
    private String replacement;

    public ReplacePathRegexMiddlewareOptions() {
    }

    public String getRegex() {
        return regex;
    }

    public String getReplacement() {
        return replacement;
    }

    @Override
    public ReplacePathRegexMiddlewareOptions clone() {
        try {
            return (ReplacePathRegexMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
