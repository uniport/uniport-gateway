package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = RedirectRegexMiddlewareOptions.Builder.class)
public abstract class AbstractRedirectRegexMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REGEX)
    public abstract String getRegex();

    @JsonProperty(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REPLACEMENT)
    public abstract String getReplacement();
}
