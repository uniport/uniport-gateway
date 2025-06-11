package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = OpenTelemetryMiddlewareOptions.Builder.class)
public abstract class AbstractOpenTelemetryMiddlewareOptions implements MiddlewareOptionsModel {

}
