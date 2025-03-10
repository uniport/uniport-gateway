package com.inventage.portal.gateway.proxy.middleware.csp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = CSPViolationReportingServerMiddlewareOptions.Builder.class)
public abstract class AbstractCSPViolationReportingServerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CSPViolationReportingServerMiddlewareFactory.CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL)
    public abstract String getLogLevel();

}
