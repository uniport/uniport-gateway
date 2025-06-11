package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import io.micrometer.common.lang.Nullable;
import io.vertx.ext.web.handler.CSRFHandler;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CSRFMiddlewareOptions.Builder.class)
public abstract class AbstractCSRFMiddlewareOptions implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSRFMiddlewareOptions.class);

    public static final String DEFAULT_COOKIE_NAME = CSRFHandler.DEFAULT_COOKIE_NAME;
    public static final String DEFAULT_COOKIE_PATH = CSRFHandler.DEFAULT_COOKIE_PATH;
    public static final boolean DEFAULT_COOKIE_SECURE = true;
    public static final String DEFAULT_HEADER_NAME = CSRFHandler.DEFAULT_HEADER_NAME;
    public static final boolean DEFAULT_NAG_HTTPS = true;
    public static final String DEFAULT_ORIGIN = null;
    public static final long DEFAULT_TIMEOUT_IN_MINUTES = 15;

    @Default
    @JsonProperty(CSRFMiddlewareFactory.COOKIE)
    public CookieOptions getCookie() {
        logDefault(LOGGER, CSRFMiddlewareFactory.COOKIE);
        return CookieOptions.builder().build();
    }

    @Default
    @JsonProperty(CSRFMiddlewareFactory.HEADER_NAME)
    public String getHeaderName() {
        logDefault(LOGGER, CSRFMiddlewareFactory.HEADER_NAME, DEFAULT_HEADER_NAME);
        return DEFAULT_HEADER_NAME;
    }

    @Default
    @JsonProperty(CSRFMiddlewareFactory.NAG_HTTPS)
    public boolean nagHTTPs() {
        logDefault(LOGGER, CSRFMiddlewareFactory.NAG_HTTPS, DEFAULT_NAG_HTTPS);
        return DEFAULT_NAG_HTTPS;
    }

    @Nullable
    @Default
    @JsonProperty(CSRFMiddlewareFactory.ORIGIN)
    public String getOrigin() {
        logDefault(LOGGER, CSRFMiddlewareFactory.ORIGIN, DEFAULT_ORIGIN);
        return DEFAULT_ORIGIN;
    }

    @Default
    @JsonProperty(CSRFMiddlewareFactory.TIMEOUT_IN_MINUTES)
    public long getTimeoutMinutes() {
        logDefault(LOGGER, CSRFMiddlewareFactory.TIMEOUT_IN_MINUTES, DEFAULT_TIMEOUT_IN_MINUTES);
        return DEFAULT_TIMEOUT_IN_MINUTES;
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = CookieOptions.Builder.class)
    public abstract static class AbstractCookieOptions implements MiddlewareOptionsModel {

        @Default
        @JsonProperty(CSRFMiddlewareFactory.COOKIE_NAME)
        public String getName() {
            logDefault(LOGGER, CSRFMiddlewareFactory.COOKIE_NAME, DEFAULT_COOKIE_NAME);
            return DEFAULT_COOKIE_NAME;
        }

        @Default
        @JsonProperty(CSRFMiddlewareFactory.COOKIE_PATH)
        public String getPath() {
            logDefault(LOGGER, CSRFMiddlewareFactory.COOKIE_PATH, DEFAULT_COOKIE_PATH);
            return DEFAULT_COOKIE_PATH;
        }

        @Default
        @JsonProperty(CSRFMiddlewareFactory.COOKIE_SECURE)
        public boolean isSecure() {
            logDefault(LOGGER, CSRFMiddlewareFactory.COOKIE_SECURE, DEFAULT_COOKIE_SECURE);
            return DEFAULT_COOKIE_SECURE;
        }
    }
}
