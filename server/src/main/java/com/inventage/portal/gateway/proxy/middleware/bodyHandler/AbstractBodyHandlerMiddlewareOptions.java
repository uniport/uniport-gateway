package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = BodyHandlerMiddlewareOptions.Builder.class)
public abstract class AbstractBodyHandlerMiddlewareOptions implements MiddlewareOptionsModel {

}
