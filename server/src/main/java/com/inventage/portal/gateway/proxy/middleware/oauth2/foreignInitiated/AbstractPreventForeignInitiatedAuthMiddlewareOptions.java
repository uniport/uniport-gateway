package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

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
@JsonDeserialize(builder = PreventForeignInitiatedAuthMiddlewareOptions.Builder.class)
public abstract class AbstractPreventForeignInitiatedAuthMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreventForeignInitiatedAuthMiddlewareOptions.class);

    @Default
    @JsonProperty(PreventForeignInitiatedAuthMiddlewareFactory.PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT)
    public String getRedirectURI() {
        logDefault(LOGGER, PreventForeignInitiatedAuthMiddlewareFactory.PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT, PreventForeignInitiatedAuthMiddlewareFactory.DEFAULT_REDIRECT_URI);
        return PreventForeignInitiatedAuthMiddlewareFactory.DEFAULT_REDIRECT_URI;
    }

}
