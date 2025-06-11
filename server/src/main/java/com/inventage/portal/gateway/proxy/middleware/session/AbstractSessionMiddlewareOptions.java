package com.inventage.portal.gateway.proxy.middleware.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import io.vertx.core.http.CookieSameSite;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = SessionMiddlewareOptions.Builder.class)
public abstract class AbstractSessionMiddlewareOptions implements MiddlewareOptionsModel {

    // session
    public static final int DEFAULT_SESSION_ID_MINIMUM_LENGTH = 32;
    public static final int DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE = 15;
    public static final String DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI = null;
    public static final boolean DEFAULT_NAG_HTTPS = true;

    // session cookie
    public static final String DEFAULT_SESSION_COOKIE_NAME = "uniport.session";
    public static final boolean DEFAULT_SESSION_COOKIE_HTTP_ONLY = true;
    public static final boolean DEFAULT_SESSION_COOKIE_SECURE = false;
    public static final CookieSameSite DEFAULT_SESSION_COOKIE_SAME_SITE = CookieSameSite.STRICT;

    // session lifetime
    public static final boolean DEFAULT_SESSION_LIFETIME_HEADER = false;
    public static final String DEFAULT_SESSION_LIFETIME_HEADER_NAME = "x-uniport-session-lifetime";

    public static final boolean DEFAULT_SESSION_LIFETIME_COOKIE = false;
    public static final String DEFAULT_SESSION_LIFETIME_COOKIE_NAME = "uniport.session-lifetime";
    public static final String DEFAULT_SESSION_LIFETIME_COOKIE_PATH = "/";
    public static final boolean DEFAULT_SESSION_LIFETIME_COOKIE_HTTP_ONLY = false; // false := cookie must be accessible by client side scripts
    public static final boolean DEFAULT_SESSION_LIFETIME_COOKIE_SECURE = false;
    public static final CookieSameSite DEFAULT_SESSION_LIFETIME_COOKIE_SAME_SITE = CookieSameSite.STRICT; // prevent warnings in Firefox console

    // session store
    public static final int DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS = 5 * 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddlewareOptions.class);

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES)
    public int getIdleTimeoutMinutes() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES, DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE);
        return DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH)
    public int getIdMinLength() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH, DEFAULT_SESSION_ID_MINIMUM_LENGTH);
        return DEFAULT_SESSION_ID_MINIMUM_LENGTH;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.NAG_HTTPS)
    public boolean nagHttps() {
        logDefault(LOGGER, SessionMiddlewareFactory.NAG_HTTPS, DEFAULT_NAG_HTTPS);
        return DEFAULT_NAG_HTTPS;
    }

    @Default
    @Nullable
    @JsonProperty(SessionMiddlewareFactory.IGNORE_SESSION_TIMEOUT_RESET_FOR_URI)
    public String getIgnoreSessionTimeoutResetForURI() {
        logDefault(LOGGER, SessionMiddlewareFactory.IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI);
        return DEFAULT_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI;
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
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_LIFETIME_HEADER, DEFAULT_SESSION_LIFETIME_HEADER);
        return DEFAULT_SESSION_LIFETIME_HEADER;
    }

    @Default
    public String getLifetimeHeader() {
        return DEFAULT_SESSION_LIFETIME_HEADER_NAME;
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.SESSION_LIFETIME_COOKIE)
    public boolean useLifetimeCookie() {
        logDefault(LOGGER, SessionMiddlewareFactory.SESSION_LIFETIME_COOKIE, DEFAULT_SESSION_LIFETIME_COOKIE);
        return DEFAULT_SESSION_LIFETIME_COOKIE;
    }

    @Default
    public LifetimeCookieOptions getLifetimeCookie() {
        return LifetimeCookieOptions.builder().build();
    }

    @Default
    @JsonProperty(SessionMiddlewareFactory.CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS)
    public Integer getClusteredSessionStoreRetryTimeoutMs() {
        logDefault(LOGGER, SessionMiddlewareFactory.CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS, DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS);
        return DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILLISECONDS;
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = SessionCookieOptions.Builder.class)
    public abstract static class AbstractSessionCookieOptions implements MiddlewareOptionsModel {

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_NAME)
        public String getName() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_NAME, DEFAULT_SESSION_COOKIE_NAME);
            return DEFAULT_SESSION_COOKIE_NAME;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY)
        public boolean isHTTPOnly() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY, DEFAULT_SESSION_COOKIE_HTTP_ONLY);
            return DEFAULT_SESSION_COOKIE_HTTP_ONLY;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SECURE)
        public boolean isSecure() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_SECURE, DEFAULT_SESSION_COOKIE_SECURE);
            return DEFAULT_SESSION_COOKIE_SECURE;
        }

        @Default
        @JsonProperty(SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE)
        public CookieSameSite getSameSite() {
            logDefault(LOGGER, SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE, DEFAULT_SESSION_COOKIE_SAME_SITE);
            return DEFAULT_SESSION_COOKIE_SAME_SITE;
        }
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = LifetimeCookieOptions.Builder.class)
    public abstract static class AbstractLifetimeCookieOptions implements MiddlewareOptionsModel {

        @Default
        public String getName() {
            return DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
        }

        @Default
        public String getPath() {
            return DEFAULT_SESSION_LIFETIME_COOKIE_PATH;
        }

        @Default
        public boolean isHTTPOnly() {
            return DEFAULT_SESSION_LIFETIME_COOKIE_HTTP_ONLY;
        }

        @Default
        public boolean isSecure() {
            return DEFAULT_SESSION_LIFETIME_COOKIE_SECURE;
        }

        @Default
        public CookieSameSite getSameSite() {
            return DEFAULT_SESSION_LIFETIME_COOKIE_SAME_SITE;
        }
    }
}
