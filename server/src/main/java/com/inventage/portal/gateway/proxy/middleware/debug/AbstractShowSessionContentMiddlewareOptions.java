package com.inventage.portal.gateway.proxy.middleware.debug;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ShowSessionContentMiddlewareOptions.Builder.class)
public abstract class AbstractShowSessionContentMiddlewareOptions implements MiddlewareOptionsModel {

    public static final String DEFAULT_INSTANCE_NAME = "unknown";

}
