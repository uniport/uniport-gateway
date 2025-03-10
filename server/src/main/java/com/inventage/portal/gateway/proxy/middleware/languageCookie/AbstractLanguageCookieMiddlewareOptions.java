package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = LanguageCookieMiddlewareOptions.Builder.class)
public abstract class AbstractLanguageCookieMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME)
    public String getCookieName() {
        return LanguageCookieMiddlewareFactory.DEFAULT_LANGUAGE_COOKIE_NAME;
    }

}
