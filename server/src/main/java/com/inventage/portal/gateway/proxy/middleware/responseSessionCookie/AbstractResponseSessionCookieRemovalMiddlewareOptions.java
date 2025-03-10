package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = ResponseSessionCookieRemovalMiddlewareOptions.Builder.class)
public abstract class AbstractResponseSessionCookieRemovalMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(ResponseSessionCookieRemovalMiddlewareFactory.RESPONSE_SESSION_COOKIE_REMOVAL_NAME)
    public String getSessionCookieName() {
        return ResponseSessionCookieRemovalMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    }
}
