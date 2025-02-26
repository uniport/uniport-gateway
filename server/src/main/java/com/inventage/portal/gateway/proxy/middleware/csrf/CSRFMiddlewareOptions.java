package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CSRFMiddlewareOptions implements GatewayMiddlewareOptions {

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

    public CSRFMiddlewareOptions() {
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

    public static final class CookieOptions implements GatewayMiddlewareOptions {

        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_NAME)
        private String name;

        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_PATH)
        private String path;

        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_SECURE)
        private Boolean secure;

        private CookieOptions() {
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
    }
}
