package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = BackChannelLogoutMiddlewareOptions.Builder.class)
public abstract class AbstractBackChannelLogoutMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

}
