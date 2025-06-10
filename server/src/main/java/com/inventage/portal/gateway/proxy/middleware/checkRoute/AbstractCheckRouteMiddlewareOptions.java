package com.inventage.portal.gateway.proxy.middleware.checkRoute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = CheckRouteMiddlewareOptions.Builder.class)
public abstract class AbstractCheckRouteMiddlewareOptions implements GatewayMiddlewareOptions {

}
