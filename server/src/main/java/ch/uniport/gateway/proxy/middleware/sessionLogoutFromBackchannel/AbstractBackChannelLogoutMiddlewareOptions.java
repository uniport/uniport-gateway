package ch.uniport.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import ch.uniport.gateway.proxy.middleware.ModelStyle;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = BackChannelLogoutMiddlewareOptions.Builder.class)
public abstract class AbstractBackChannelLogoutMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

}
