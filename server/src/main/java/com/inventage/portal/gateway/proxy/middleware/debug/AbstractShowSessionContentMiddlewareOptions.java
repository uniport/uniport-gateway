package com.inventage.portal.gateway.proxy.middleware.debug;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = ShowSessionContentMiddlewareOptions.Builder.class)
public abstract class AbstractShowSessionContentMiddlewareOptions implements GatewayMiddlewareOptions {

}
