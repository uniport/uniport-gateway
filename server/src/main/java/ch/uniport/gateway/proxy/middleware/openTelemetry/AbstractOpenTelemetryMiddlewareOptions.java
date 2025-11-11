package ch.uniport.gateway.proxy.middleware.openTelemetry;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = OpenTelemetryMiddlewareOptions.Builder.class)
public abstract class AbstractOpenTelemetryMiddlewareOptions implements MiddlewareOptionsModel {

}
