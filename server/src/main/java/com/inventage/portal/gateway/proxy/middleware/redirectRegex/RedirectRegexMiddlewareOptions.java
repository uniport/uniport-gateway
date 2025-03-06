package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = RedirectRegexMiddlewareOptions.Builder.class)
public final class RedirectRegexMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REGEX)
    private String regex;

    @JsonProperty(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REPLACEMENT)
    private String replacement;

    public static Builder builder() {
        return new Builder();
    }

    private RedirectRegexMiddlewareOptions(Builder builder) {
        if (builder.regex == null) {
            throw new IllegalArgumentException("regex is required");
        }
        if (builder.replacement == null) {
            throw new IllegalArgumentException("replacement is required");
        }
        this.regex = builder.regex;
        this.replacement = builder.replacement;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private String regex;
        private String replacement;

        public Builder witRegex(String regex) {
            this.regex = regex;
            return this;
        }

        public Builder witReplacement(String replacement) {
            this.replacement = replacement;
            return this;
        }

        public RedirectRegexMiddlewareOptions build() {
            return new RedirectRegexMiddlewareOptions(this);
        }
    }
}
