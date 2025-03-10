package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ReplacedSessionCookieDetectionMiddlewareOptions.Builder.class)
public abstract class AbstractReplacedSessionCookieDetectionMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME)
    public String getCookieName() {
        return ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    }

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS)
    public int getWaitBeforeRetryMs() {
        return ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_WAIT_BEFORE_RETRY_MS;
    }

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES)
    public int getMaxRetries() {
        return ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_MAX_REDIRECT_RETRIES;
    }
}
