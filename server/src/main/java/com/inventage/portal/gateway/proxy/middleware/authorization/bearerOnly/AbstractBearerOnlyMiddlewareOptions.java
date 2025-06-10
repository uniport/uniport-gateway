package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = BearerOnlyMiddlewareOptions.Builder.class)
public abstract class AbstractBearerOnlyMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareOptions.class);

    @Default
    @JsonProperty(BearerOnlyMiddlewareFactory.OPTIONAL)
    public String isOptional() {
        logDefault(LOGGER, BearerOnlyMiddlewareFactory.OPTIONAL, BearerOnlyMiddlewareFactory.DEFAULT_OPTIONAL);
        return BearerOnlyMiddlewareFactory.DEFAULT_OPTIONAL;
    }
}
