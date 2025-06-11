package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = RedirectRegexMiddlewareOptions.Builder.class)
public abstract class AbstractRedirectRegexMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(RedirectRegexMiddlewareFactory.REGEX)
    public abstract String getRegex();

    @JsonProperty(RedirectRegexMiddlewareFactory.REPLACEMENT)
    public abstract String getReplacement();
}
