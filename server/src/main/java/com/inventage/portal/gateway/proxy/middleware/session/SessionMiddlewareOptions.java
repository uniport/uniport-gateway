package com.inventage.portal.gateway.proxy.middleware.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = SessionMiddlewareOptions.Builder.class)
public final class SessionMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES)
    private Integer idleTimeoutMinutes;

    @JsonProperty(SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH)
    private Integer idMinLength;

    @JsonProperty(SessionMiddlewareFactory.SESSION_NAG_HTTPS)
    private Boolean nagHttps;

    @JsonProperty(SessionMiddlewareFactory.SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI)
    private String ignoreSessionTimeoutResetForURI;

    @JsonProperty(SessionMiddlewareFactory.SESSION_LIFETIME_COOKIE)
    private Boolean useCookie;

    @JsonProperty(SessionMiddlewareFactory.SESSION_LIFETIME_HEADER)
    private Boolean useHeader;

    @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE)
    private CookieOptions cookie;

    @JsonProperty(SessionMiddlewareFactory.CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS)
    private Integer clusteredSessionStoreRetryTimeoutMs;

    public static Builder builder() {
        return new Builder();
    }

    private SessionMiddlewareOptions(Builder builder) {
        this.idleTimeoutMinutes = builder.idleTimeoutMinutes;
        this.idMinLength = builder.idMinLength;
        this.nagHttps = builder.nagHttps;
        this.ignoreSessionTimeoutResetForURI = builder.ignoreSessionTimeoutResetForURI;
        this.useCookie = builder.useCookie;
        this.useHeader = builder.useHeader;
        this.cookie = builder.cookie;
        this.clusteredSessionStoreRetryTimeoutMs = builder.clusteredSessionStoreRetryTimeoutMs;
    }

    public Integer getIdleTimeoutMinutes() {
        return idleTimeoutMinutes;
    }

    public Integer getIdMinLength() {
        return idMinLength;
    }

    public Boolean nagHttps() {
        return nagHttps;
    }

    public String getIgnoreSessionTimeoutResetForURI() {
        return ignoreSessionTimeoutResetForURI;
    }

    public Boolean useLifetimeCookie() {
        return useCookie;
    }

    public Boolean useLifetimeHeader() {
        return useHeader;
    }

    public CookieOptions getCookie() {
        return cookie;
    }

    public Integer getClusteredSessionStoreRetryTimeoutMs() {
        return clusteredSessionStoreRetryTimeoutMs;
    }

    @Override
    public SessionMiddlewareOptions clone() {
        try {
            final SessionMiddlewareOptions options = (SessionMiddlewareOptions) super.clone();
            options.cookie = cookie == null ? null : cookie.clone();
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonDeserialize(builder = CookieOptions.Builder.class)
    public static final class CookieOptions implements GatewayMiddlewareOptions {

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_NAME)
        private String name;

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY)
        private Boolean httpOnly;

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SECURE)
        private Boolean secure;

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE)
        private String sameSite;

        private CookieOptions(Builder builder) {
            this.name = builder.name;
            this.httpOnly = builder.httpOnly;
            this.secure = builder.secure;
            this.sameSite = builder.sameSite;
        }

        public String getName() {
            return name;
        }

        public Boolean isHTTPOnly() {
            return httpOnly;
        }

        public Boolean isSecure() {
            return secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        @Override
        public CookieOptions clone() {
            try {
                return (CookieOptions) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @JsonPOJOBuilder
        public static final class Builder {

            private String name;
            private Boolean httpOnly;
            private Boolean secure;
            private String sameSite;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withHttpOnly(Boolean httpOnly) {
                this.httpOnly = httpOnly;
                return this;
            }

            public Builder withSecure(Boolean secure) {
                this.secure = secure;
                return this;
            }

            public Builder withSameSite(String sameSite) {
                this.sameSite = sameSite;
                return this;
            }

            public CookieOptions build() {
                return new CookieOptions(this);
            }
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private Integer idleTimeoutMinutes;
        private Integer idMinLength;
        private Boolean nagHttps;
        private String ignoreSessionTimeoutResetForURI;
        private Boolean useCookie;
        private Boolean useHeader;
        private CookieOptions cookie;
        private Integer clusteredSessionStoreRetryTimeoutMs;

        public Builder withIdleTimeoutMinutes(Integer idleTimeoutMinutes) {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
            return this;
        }

        public Builder withIdMinLength(Integer idMinLength) {
            this.idMinLength = idMinLength;
            return this;
        }

        public Builder withNagHttps(Boolean nagHttps) {
            this.nagHttps = nagHttps;
            return this;
        }

        public Builder withIgnoreSessionTimeoutResetForURI(String ignoreSessionTimeoutResetForURI) {
            this.ignoreSessionTimeoutResetForURI = ignoreSessionTimeoutResetForURI;
            return this;
        }

        public Builder withUseCookie(Boolean useCookie) {
            this.useCookie = useCookie;
            return this;
        }

        public Builder withUseHeader(Boolean useHeader) {
            this.useHeader = useHeader;
            return this;
        }

        public Builder withCookie(CookieOptions cookie) {
            this.cookie = cookie;
            return this;
        }

        public Builder withClusteredSessionStoreRetryTimeoutMs(Integer clusteredSessionStoreRetryTimeoutMs) {
            this.clusteredSessionStoreRetryTimeoutMs = clusteredSessionStoreRetryTimeoutMs;
            return this;
        }

        public SessionMiddlewareOptions build() {
            return new SessionMiddlewareOptions(this);
        }
    }
}
