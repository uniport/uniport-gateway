package com.inventage.portal.gateway.proxy.middleware.debug;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ShowSessionContentMiddlewareOptions.Builder.class)
public abstract class AbstractShowSessionContentMiddlewareOptions implements GatewayMiddlewareOptions {

}
