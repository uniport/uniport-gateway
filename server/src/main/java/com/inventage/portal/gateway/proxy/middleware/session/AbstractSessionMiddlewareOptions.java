package com.inventage.portal.gateway.proxy.middleware.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import io.vertx.core.http.CookieSameSite;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = SessionMiddlewareOptions.Builder.class)
public abstract class AbstractSessionMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES)
    public int getIdleTimeoutMinutes() {
        return SessionMiddlewareFactory.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH)
    public int getIdMinLength() {
        return SessionMiddlewareFactory.DEFAULT_SESSION_ID_MINIMUM_LENGTH;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_NAG_HTTPS)
    public boolean nagHttps() {
        return SessionMiddlewareFactory.DEFAULT_NAG_HTTPS;
    }

    @Default
    @Nullable
    @JsonProperty(SessionMiddlewareFactory.SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI)
    public String getIgnoreSessionTimeoutResetForURI() {
        return SessionMiddlewareFactory.DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_LIFETIME_COOKIE)
    public boolean useLifetimeCookie() {
        return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_LIFETIME_HEADER)
    public boolean useLifetimeHeader() {
        return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE)
    public CookieOptions getCookie() {
        return CookieOptions.builder()
            .withName(SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME)
            .withHTTPOnly(SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_HTTP_ONLY)
            .withSecure(SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SECURE)
            .withSameSite(SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SAME_SITE)
            .build();
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS)
    public Integer getClusteredSessionStoreRetryTimeoutMs() {
        return SessionMiddlewareFactory.DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS;
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = CookieOptions.Builder.class)
    public abstract static class AbstractCookieOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_NAME)
        public String getName() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY)
        public boolean isHTTPOnly() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_HTTP_ONLY;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SECURE)
        public boolean isSecure() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SECURE;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE)
        public CookieSameSite getSameSite() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SAME_SITE;
        }
    }
}
