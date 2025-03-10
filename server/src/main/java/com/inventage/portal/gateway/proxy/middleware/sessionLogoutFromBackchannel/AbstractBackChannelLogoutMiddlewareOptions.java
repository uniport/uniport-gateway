package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = BackChannelLogoutMiddlewareOptions.Builder.class)
public abstract class AbstractBackChannelLogoutMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

}
