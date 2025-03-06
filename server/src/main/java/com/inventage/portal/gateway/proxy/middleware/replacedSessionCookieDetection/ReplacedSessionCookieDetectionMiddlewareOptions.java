package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ReplacedSessionCookieDetectionMiddlewareOptions.Builder.class)
public final class ReplacedSessionCookieDetectionMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME)
    private String cookieName;

    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS)
    private Integer waitBeforeRetryMs;

    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES)
    private Integer maxRetries;

    public static Builder builder() {
        return new Builder();
    }

    private ReplacedSessionCookieDetectionMiddlewareOptions(Builder builder) {
        this.cookieName = builder.cookieName;
        this.waitBeforeRetryMs = builder.waitBeforeRetryMs;
        this.maxRetries = builder.maxRetries;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private String cookieName;
        private Integer waitBeforeRetryMs;
        private Integer maxRetries;

        public Builder withCookieName(String cookieName) {
            this.cookieName = cookieName;
            return this;
        }

        public Builder withWaitBeforeRetryMs(Integer waitBeforeRetryMs) {
            this.waitBeforeRetryMs = waitBeforeRetryMs;
            return this;
        }

        public Builder withMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public ReplacedSessionCookieDetectionMiddlewareOptions build() {
            return new ReplacedSessionCookieDetectionMiddlewareOptions(this);
        }
    }
}
