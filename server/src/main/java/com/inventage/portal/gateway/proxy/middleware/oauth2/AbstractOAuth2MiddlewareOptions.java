package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = OAuth2MiddlewareOptions.Builder.class)
public abstract class AbstractOAuth2MiddlewareOptions extends AbstractOAuth2MiddlewareOptionsBase {

}
