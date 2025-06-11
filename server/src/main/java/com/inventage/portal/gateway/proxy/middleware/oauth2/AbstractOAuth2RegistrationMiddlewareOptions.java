package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = OAuth2RegistrationMiddlewareOptions.Builder.class)
public abstract class AbstractOAuth2RegistrationMiddlewareOptions extends AbstractOAuth2MiddlewareOptionsBase {

}
