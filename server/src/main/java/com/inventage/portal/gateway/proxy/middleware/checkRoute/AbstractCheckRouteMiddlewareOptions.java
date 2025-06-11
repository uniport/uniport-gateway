package com.inventage.portal.gateway.proxy.middleware.checkRoute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CheckRouteMiddlewareOptions.Builder.class)
public abstract class AbstractCheckRouteMiddlewareOptions implements MiddlewareOptionsModel {

}
