package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = OAuth2MiddlewareOptions.Builder.class)
public abstract class AbstractOAuth2MiddlewareOptions extends AbstractOAuth2MiddlewareOptionsBase {

}
