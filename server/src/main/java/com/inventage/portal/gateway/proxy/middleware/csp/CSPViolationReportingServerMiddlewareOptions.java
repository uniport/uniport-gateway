package com.inventage.portal.gateway.proxy.middleware.csp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CSPViolationReportingServerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CSPViolationReportingServerMiddlewareFactory.CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL)
    private String logLevel;

    public CSPViolationReportingServerMiddlewareOptions() {
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
}
