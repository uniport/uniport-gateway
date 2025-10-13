package ch.uniport.gateway.proxy.middleware.authorization.checkJwt;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CheckJWTMiddlewareOptions.Builder.class)
public abstract class AbstractCheckJWTMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @JsonProperty(CheckJWTMiddlewareFactory.SESSION_SCOPE)
    public abstract String getSessionScope();
}
