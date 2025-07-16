package ch.uniport.gateway.proxy.middleware.checkRoute;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CheckRouteMiddlewareOptions.Builder.class)
public abstract class AbstractCheckRouteMiddlewareOptions implements MiddlewareOptionsModel {

}
