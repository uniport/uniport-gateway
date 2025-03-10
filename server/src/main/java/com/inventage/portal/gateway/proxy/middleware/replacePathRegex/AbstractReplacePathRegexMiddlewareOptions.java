package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = ReplacePathRegexMiddlewareOptions.Builder.class)
public abstract class AbstractReplacePathRegexMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REGEX)
    public abstract String getRegex();

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REPLACEMENT)
    public abstract String getReplacement();

}
