package com.inventage.portal.gateway.proxy.middleware.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.http.CookieSameSite;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = SessionMiddlewareOptions.Builder.class)
public abstract class AbstractSessionMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddlewareOptions.class);

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES)
    public int getIdleTimeoutMinutes() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES, SessionMiddlewareFactory.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE);
        return SessionMiddlewareFactory.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH)
    public int getIdMinLength() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH, SessionMiddlewareFactory.DEFAULT_SESSION_ID_MINIMUM_LENGTH);
        return SessionMiddlewareFactory.DEFAULT_SESSION_ID_MINIMUM_LENGTH;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.NAG_HTTPS)
    public boolean nagHttps() {
        logDefault(LOGGER, SessionMiddlewareFactory.NAG_HTTPS, SessionMiddlewareFactory.DEFAULT_NAG_HTTPS);
        return SessionMiddlewareFactory.DEFAULT_NAG_HTTPS;
    }

    @Default
    @Nullable
    @JsonProperty(SessionMiddlewareFactory.IGNORE_SESSION_TIMEOUT_RESET_FOR_URI)
    public String getIgnoreSessionTimeoutResetForURI() {
        logDefault(LOGGER, SessionMiddlewareFactory.IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, SessionMiddlewareFactory.DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI);
        return SessionMiddlewareFactory.DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE)
    public SessionCookieOptions getSessionCookie() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE);
        return SessionCookieOptions.builder().build();
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_LIFETIME_HEADER)
    public boolean useLifetimeHeader() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_LIFETIME_HEADER, SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER);
        return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER;
    }

    @Default
    public String getLifetimeHeader() {
        return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER_NAME;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_LIFETIME_COOKIE)
    public boolean useLifetimeCookie() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_LIFETIME_COOKIE, SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE);
        return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE;
    }

    @Default
    public LifetimeCookieOptions getLifetimeCookie() {
        return LifetimeCookieOptions.builder().build();
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS)
    public Integer getClusteredSessionStoreRetryTimeoutMs() {
        logDefault(LOGGER, SessionMiddlewareFactory.CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS, SessionMiddlewareFactory.DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS);
        return SessionMiddlewareFactory.DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS;
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = SessionCookieOptions.Builder.class)
    public abstract static class AbstractSessionCookieOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_NAME)
        public String getName() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_NAME, SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME);
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY)
        public boolean isHTTPOnly() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY, SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_HTTP_ONLY);
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_HTTP_ONLY;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SECURE)
        public boolean isSecure() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_SECURE, SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SECURE);
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SECURE;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE)
        public CookieSameSite getSameSite() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE, SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SAME_SITE);
            return SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SAME_SITE;
        }
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = LifetimeCookieOptions.Builder.class)
    public abstract static class AbstractLifetimeCookieOptions implements GatewayMiddlewareOptions {

        @Default
        public String getName() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
        }

        @Default
        public String getPath() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_PATH;
        }

        @Default
        public boolean isHTTPOnly() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_HTTP_ONLY;
        }

        @Default
        public boolean isSecure() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_SECURE;
        }

        @Default
        public CookieSameSite getSameSite() {
            return SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_SAME_SITE;
        }
    }
}
