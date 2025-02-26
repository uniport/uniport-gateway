package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplacedSessionCookieDetectionMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME)
    private String cookieName;

    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS)
    private Integer waitBeforeRetryMs;

    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES)
    private Integer maxRetries;

    public ReplacedSessionCookieDetectionMiddlewareOptions() {
    }

    public String getCookieName() {
        return cookieName;
    }

    public Integer getWaitBeforeRetryMs() {
        return waitBeforeRetryMs;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    @Override
    public ReplacedSessionCookieDetectionMiddlewareOptions clone() {
        try {
            return (ReplacedSessionCookieDetectionMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
