package ch.uniport.gateway.proxy.middleware.authorization.passAuthorization;

import ch.uniport.gateway.proxy.middleware.ModelStyle;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = PassAuthorizationMiddlewareOptions.Builder.class)
public abstract class AbstractPassAuthorizationMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @JsonProperty(PassAuthorizationMiddlewareFactory.SESSION_SCOPE)
    public abstract String getSessionScope();
}
