package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackChannelLogoutMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    public BackChannelLogoutMiddlewareOptions() {
    }

    @Override
    public BackChannelLogoutMiddlewareOptions clone() {
        return (BackChannelLogoutMiddlewareOptions) super.clone();
    }
}
