package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = OpenTelemetryMiddlewareOptions.Builder.class)
public abstract class AbstractOpenTelemetryMiddlewareOptions implements GatewayMiddlewareOptions {

}
