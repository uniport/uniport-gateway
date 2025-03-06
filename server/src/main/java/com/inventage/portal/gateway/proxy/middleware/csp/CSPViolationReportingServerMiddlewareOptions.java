package com.inventage.portal.gateway.proxy.middleware.csp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = CSPViolationReportingServerMiddlewareOptions.Builder.class)
public final class CSPViolationReportingServerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CSPViolationReportingServerMiddlewareFactory.CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL)
    private String logLevel;

    public static Builder builder() {
        return new Builder();
    }

    private CSPViolationReportingServerMiddlewareOptions(Builder builder) {
        if (builder.logLevel == null) {
            throw new IllegalArgumentException("log level is required");
        }
        this.logLevel = builder.logLevel;
    }

    public String getLogLevel() {
        return logLevel;
    }

    @Override
    public CSPViolationReportingServerMiddlewareOptions clone() {
        try {
            return (CSPViolationReportingServerMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String logLevel;

        public Builder withLogLevel(String logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public CSPViolationReportingServerMiddlewareOptions build() {
            return new CSPViolationReportingServerMiddlewareOptions(this);
        }
    }
}
