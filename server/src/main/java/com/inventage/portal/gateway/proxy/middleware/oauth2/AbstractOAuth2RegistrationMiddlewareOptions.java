package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = OAuth2RegistrationMiddlewareOptions.Builder.class)
public abstract class AbstractOAuth2RegistrationMiddlewareOptions extends AbstractOAuth2MiddlewareOptions {

}
