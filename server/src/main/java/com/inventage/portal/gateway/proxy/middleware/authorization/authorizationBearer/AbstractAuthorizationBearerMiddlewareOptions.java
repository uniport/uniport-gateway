package com.inventage.portal.gateway.proxy.middleware.authorization.authorizationBearer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = AuthorizationBearerMiddlewareOptions.Builder.class)
public abstract class AbstractAuthorizationBearerMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(AuthorizationBearerMiddlewareFactory.SESSION_SCOPE)
    public abstract String getSessionScope();
}
