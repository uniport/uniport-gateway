package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = BodyHandlerMiddlewareOptions.Builder.class)
public abstract class AbstractBodyHandlerMiddlewareOptions implements GatewayMiddlewareOptions {

}
