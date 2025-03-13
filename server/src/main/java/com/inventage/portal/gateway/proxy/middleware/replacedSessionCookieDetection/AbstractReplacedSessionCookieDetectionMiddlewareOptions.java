package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ReplacedSessionCookieDetectionMiddlewareOptions.Builder.class)
public abstract class AbstractReplacedSessionCookieDetectionMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacedSessionCookieDetectionMiddlewareOptions.class);

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME)
    public String getCookieName() {
        logDefault(LOGGER, ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME, ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME);
        return ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    }

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS)
    public int getWaitBeforeRetryMs() {
        logDefault(LOGGER, ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS, ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_WAIT_BEFORE_RETRY_MS);
        return ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_WAIT_BEFORE_RETRY_MS;
    }

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES)
    public int getMaxRetries() {
        logDefault(LOGGER, ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES, ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_MAX_REDIRECT_RETRIES);
        return ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_MAX_REDIRECT_RETRIES;
    }
}
