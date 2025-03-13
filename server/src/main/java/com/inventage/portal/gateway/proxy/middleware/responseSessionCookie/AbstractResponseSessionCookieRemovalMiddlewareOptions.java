package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ResponseSessionCookieRemovalMiddlewareOptions.Builder.class)
public abstract class AbstractResponseSessionCookieRemovalMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddlewareOptions.class);

    @Default
    @JsonProperty(ResponseSessionCookieRemovalMiddlewareFactory.RESPONSE_SESSION_COOKIE_REMOVAL_NAME)
    public String getSessionCookieName() {
        logDefault(LOGGER, ResponseSessionCookieRemovalMiddlewareFactory.RESPONSE_SESSION_COOKIE_REMOVAL_NAME, ResponseSessionCookieRemovalMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME);
        return ResponseSessionCookieRemovalMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    }
}
