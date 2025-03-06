package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = CSRFMiddlewareOptions.Builder.class)
public final class CSRFMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE)
    private CookieOptions cookie;

    @JsonProperty(CSRFMiddlewareFactory.CSRF_HEADER_NAME)
    private String headerName;

    @JsonProperty(CSRFMiddlewareFactory.CSRF_NAG_HTTPS)
    private Boolean nagHTTPs;

    @JsonProperty(CSRFMiddlewareFactory.CSRF_ORIGIN)
    private String origin;

    @JsonProperty(CSRFMiddlewareFactory.CSRF_TIMEOUT_IN_MINUTES)
    private Integer timeoutMinutes;

    public static Builder builder() {
        return new Builder();
    }

    private CSRFMiddlewareOptions(Builder builder) {
        this.cookie = builder.cookie;
        this.headerName = builder.headerName;
        this.nagHTTPs = builder.nagHTTPs;
        this.origin = builder.origin;
        this.timeoutMinutes = builder.timeoutMinutes;
    }

    public CookieOptions getCookie() {
        return cookie == null ? null : cookie.clone();
    }

    public String getHeaderName() {
        return headerName;
    }

    public Boolean nagHTTPs() {
        return nagHTTPs;
    }

    public String getOrigin() {
        return origin;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    @Override
    public CSRFMiddlewareOptions clone() {
        try {
            final CSRFMiddlewareOptions options = (CSRFMiddlewareOptions) super.clone();
            options.cookie = cookie == null ? null : cookie.clone();
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonDeserialize(builder = CookieOptions.Builder.class)
    public static final class CookieOptions implements GatewayMiddlewareOptions {

        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_NAME)
        private String name;

        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_PATH)
        private String path;

        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_SECURE)
        private Boolean secure;

        private CookieOptions(Builder builder) {
            this.name = builder.name;
            this.path = builder.path;
            this.secure = builder.secure;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public Boolean isSecure() {
            return secure;
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
            private String path;
            private Boolean secure;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withPath(String path) {
                this.path = path;
                return this;
            }

            public Builder withSecure(Boolean secure) {
                this.secure = secure;
                return this;
            }

            public CookieOptions build() {
                return new CookieOptions(this);
            }
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private CookieOptions cookie;
        private String headerName;
        private Boolean nagHTTPs;
        private String origin;
        private Integer timeoutMinutes;

        public Builder withCookie(CookieOptions cookie) {
            this.cookie = cookie;
            return this;
        }

        public Builder withHeaderName(String headerName) {
            this.headerName = headerName;
            return this;
        }

        public Builder withNagHTTPs(Boolean nagHTTPs) {
            this.nagHTTPs = nagHTTPs;
            return this;
        }

        public Builder withOrigin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder withTimeoutMinutes(Integer timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
            return this;
        }

        public CSRFMiddlewareOptions build() {
            return new CSRFMiddlewareOptions(this);
        }
    }
}
