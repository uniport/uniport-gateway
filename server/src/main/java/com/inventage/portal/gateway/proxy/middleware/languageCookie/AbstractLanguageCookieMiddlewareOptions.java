package com.inventage.portal.gateway.proxy.middleware.languageCookie;

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
@JsonDeserialize(builder = LanguageCookieMiddlewareOptions.Builder.class)
public abstract class AbstractLanguageCookieMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddlewareOptions.class);

    @Default
    @JsonProperty(LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME)
    public String getCookieName() {
        logDefault(LOGGER, LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME, LanguageCookieMiddlewareFactory.DEFAULT_LANGUAGE_COOKIE_NAME);
        return LanguageCookieMiddlewareFactory.DEFAULT_LANGUAGE_COOKIE_NAME;
    }

}
