package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = SessionBagMiddlewareOptions.Builder.class)
public final class SessionBagMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_SESSION_COOKIE_NAME)
    private String sessionCookieName;

    @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIES)
    private List<WhitelistedCookieOption> whitelistedCookies;

    public static Builder builder() {
        return new Builder();
    }

    private SessionBagMiddlewareOptions(Builder builder) {
        if (builder.whitelistedCookies == null) {
            throw new IllegalArgumentException("whitelisted cookies are required");
        }
        this.sessionCookieName = builder.sessionCookieName;
        this.whitelistedCookies = builder.whitelistedCookies;
    }

    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public List<WhitelistedCookieOption> getWhitelistedCookieOptions() {
        return whitelistedCookies.stream().map(WhitelistedCookieOption::clone).toList();
    }

    @Override
    public SessionBagMiddlewareOptions clone() {
        try {
            final SessionBagMiddlewareOptions options = (SessionBagMiddlewareOptions) super.clone();
            options.whitelistedCookies = whitelistedCookies == null ? null : whitelistedCookies.stream().map(WhitelistedCookieOption::clone).toList();
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonDeserialize(builder = WhitelistedCookieOption.Builder.class)
    public static final class WhitelistedCookieOption implements GatewayMiddlewareOptions {

        @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_NAME)
        private String name;

        @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_PATH)
        private String path;

        private WhitelistedCookieOption(Builder builder) {
            if (builder.name == null) {
                throw new IllegalArgumentException("name is required");
            }
            if (builder.path == null) {
                throw new IllegalArgumentException("path is required");
            }
            this.name = builder.name;
            this.path = builder.path;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        @Override
        public WhitelistedCookieOption clone() {
            try {
                final WhitelistedCookieOption options = (WhitelistedCookieOption) super.clone();
                return options;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @JsonPOJOBuilder
        public static final class Builder {

            private String name;
            private String path;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withPath(String path) {
                this.path = path;
                return this;
            }

            public WhitelistedCookieOption build() {
                return new WhitelistedCookieOption(this);
            }
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String sessionCookieName;
        private List<WhitelistedCookieOption> whitelistedCookies;

        public Builder withSessionCookieName(String sessionCookieName) {
            this.sessionCookieName = sessionCookieName;
            return this;
        }

        public Builder withWhitelistedCookies(List<WhitelistedCookieOption> whitelistedCookies) {
            this.whitelistedCookies = whitelistedCookies;
            return this;
        }

        public SessionBagMiddlewareOptions build() {
            return new SessionBagMiddlewareOptions(this);
        }
    }
}
