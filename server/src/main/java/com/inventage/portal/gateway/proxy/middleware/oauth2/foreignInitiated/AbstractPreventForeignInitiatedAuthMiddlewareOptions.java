package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = PreventForeignInitiatedAuthMiddlewareOptions.Builder.class)
public abstract class AbstractPreventForeignInitiatedAuthMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(PreventForeignInitiatedAuthMiddlewareFactory.PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT)
    public String getRedirectURI() {
        return PreventForeignInitiatedAuthMiddlewareFactory.DEFAULT_REDIRECT_URI;
    }

}
