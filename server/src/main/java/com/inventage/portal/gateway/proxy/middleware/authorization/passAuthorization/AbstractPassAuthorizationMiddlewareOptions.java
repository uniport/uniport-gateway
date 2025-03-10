package com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = PassAuthorizationMiddlewareOptions.Builder.class)
public abstract class AbstractPassAuthorizationMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @JsonProperty(PassAuthorizationMiddlewareFactory.PASS_AUTHORIZATION_SESSION_SCOPE)
    public abstract String getSessionScope();
}
