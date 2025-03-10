package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.middleware.proxy.AbstractProxyMiddlewareOptions;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = GatewayService.Builder.class)
public abstract class AbstractGatewayService extends AbstractProxyMiddlewareOptions {

}