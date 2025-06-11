package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = BearerOnlyMiddlewareOptions.Builder.class)
public abstract class AbstractBearerOnlyMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareOptions.class);

    public static final String DEFAULT_OPTIONAL = "false"; // TODO fix this and make it a real boolean

    @Default
    @JsonProperty(BearerOnlyMiddlewareFactory.OPTIONAL)
    public String isOptional() {
        logDefault(LOGGER, BearerOnlyMiddlewareFactory.OPTIONAL, DEFAULT_OPTIONAL);
        return DEFAULT_OPTIONAL;
    }
}
