package ch.uniport.gateway.proxy.middleware.authorization.authorizationBearer;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = AuthorizationBearerMiddlewareOptions.Builder.class)
public abstract class AbstractAuthorizationBearerMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(AuthorizationBearerMiddlewareFactory.SESSION_SCOPE)
    public abstract String getSessionScope();
}
