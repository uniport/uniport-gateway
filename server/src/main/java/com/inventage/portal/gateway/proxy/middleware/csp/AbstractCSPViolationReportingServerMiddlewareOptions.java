package com.inventage.portal.gateway.proxy.middleware.csp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;
import org.slf4j.event.Level;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CSPViolationReportingServerMiddlewareOptions.Builder.class)
public abstract class AbstractCSPViolationReportingServerMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(CSPViolationReportingServerMiddlewareFactory.LOG_LEVEL)
    public abstract Level getLogLevel();

}
