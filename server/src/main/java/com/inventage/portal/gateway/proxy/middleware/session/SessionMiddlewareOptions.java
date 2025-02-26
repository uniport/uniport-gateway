package com.inventage.portal.gateway.proxy.middleware.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionMiddlewareOptions implements GatewayMiddlewareOptions {

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

    public SessionMiddlewareOptions() {
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

    public static final class CookieOptions implements GatewayMiddlewareOptions {

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_NAME)
        private String name;

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY)
        private Boolean httpOnly;

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SECURE)
        private Boolean secure;

        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE)
        private String sameSite;

        private CookieOptions() {
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
    }
}
