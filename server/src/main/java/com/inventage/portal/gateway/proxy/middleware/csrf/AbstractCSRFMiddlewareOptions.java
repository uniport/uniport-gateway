package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = CSRFMiddlewareOptions.Builder.class)
public abstract class AbstractCSRFMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE)
    public CookieOptions getCookie() {
        return CookieOptions.builder().build();
    }

    @Default
    @JsonProperty(CSRFMiddlewareFactory.CSRF_HEADER_NAME)
    public String getHeaderName() {
        return CSRFMiddlewareFactory.DEFAULT_HEADER_NAME;
    }

    @Default
    @JsonProperty(CSRFMiddlewareFactory.CSRF_NAG_HTTPS)
    public boolean nagHTTPs() {
        return CSRFMiddlewareFactory.DEFAULT_NAG_HTTPS;
    }

    @Default
    @JsonProperty(CSRFMiddlewareFactory.CSRF_ORIGIN)
    public String getOrigin() {
        return CSRFMiddlewareFactory.DEFAULT_ORIGIN;
    }

    @Default
    @JsonProperty(CSRFMiddlewareFactory.CSRF_TIMEOUT_IN_MINUTES)
    public long getTimeoutMinutes() {
        return CSRFMiddlewareFactory.DEFAULT_TIMEOUT_IN_MINUTES;
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = CookieOptions.Builder.class)
    public abstract static class AbstractCookieOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_NAME)
        public String getName() {
            return CSRFMiddlewareFactory.DEFAULT_COOKIE_NAME;
        }

        @Default
        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_PATH)
        public String getPath() {
            return CSRFMiddlewareFactory.DEFAULT_COOKIE_PATH;
        }

        @Default
        @JsonProperty(CSRFMiddlewareFactory.CSRF_COOKIE_SECURE)
        public boolean isSecure() {
            return CSRFMiddlewareFactory.DEFAULT_COOKIE_SECURE;
        }
    }
}
