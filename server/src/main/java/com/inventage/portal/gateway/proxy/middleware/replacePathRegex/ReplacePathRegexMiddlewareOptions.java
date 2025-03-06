package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ReplacePathRegexMiddlewareOptions.Builder.class)
public final class ReplacePathRegexMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REGEX)
    private String regex;

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REPLACEMENT)
    private String replacement;

    public static Builder builder() {
        return new Builder();
    }

    private ReplacePathRegexMiddlewareOptions(Builder builder) {
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
    public ReplacePathRegexMiddlewareOptions clone() {
        try {
            return (ReplacePathRegexMiddlewareOptions) super.clone();
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

        @JsonProperty(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REPLACEMENT)
        public ReplacePathRegexMiddlewareOptions build() {
            return new ReplacePathRegexMiddlewareOptions(this);
        }
    }
}
