package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ResponseSessionCookieRemovalMiddlewareOptions.Builder.class)
public abstract class AbstractResponseSessionCookieRemovalMiddlewareOptions implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddlewareOptions.class);

    @Default
    @JsonProperty(ResponseSessionCookieRemovalMiddlewareFactory.SESSION_COOKIE_NAME)
    public String getSessionCookieName() {
        logDefault(LOGGER, ResponseSessionCookieRemovalMiddlewareFactory.SESSION_COOKIE_NAME, ResponseSessionCookieRemovalMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME);
        return ResponseSessionCookieRemovalMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    }
}
