package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ReplacePathRegexMiddlewareOptions.Builder.class)
public abstract class AbstractReplacePathRegexMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REGEX)
    public abstract String getRegex();

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACEMENT)
    public abstract String getReplacement();

}
