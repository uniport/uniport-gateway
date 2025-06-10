package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ReplacePathRegexMiddlewareOptions.Builder.class)
public abstract class AbstractReplacePathRegexMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REGEX)
    public abstract String getRegex();

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACEMENT)
    public abstract String getReplacement();

}
