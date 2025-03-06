package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = LanguageCookieMiddlewareOptions.Builder.class)
public final class LanguageCookieMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME)
    private String cookieName;

    public static Builder builder() {
        return new Builder();
    }

    private LanguageCookieMiddlewareOptions(Builder builder) {
        this.cookieName = builder.cookieName;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private String cookieName;

        public Builder withCookieName(String cookieName) {
            this.cookieName = cookieName;
            return this;
        }

        public LanguageCookieMiddlewareOptions build() {
            return new LanguageCookieMiddlewareOptions(this);
        }
    }
}
