package com.inventage.portal.gateway.proxy.middleware.authorization.authorizationBearer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = AuthorizationBearerMiddlewareOptions.Builder.class)
public abstract class AbstractAuthorizationBearerMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(AuthorizationBearerMiddlewareFactory.AUTHORIZATION_BEARER_SESSION_SCOPE)
    public abstract String getSessionScope();
}
