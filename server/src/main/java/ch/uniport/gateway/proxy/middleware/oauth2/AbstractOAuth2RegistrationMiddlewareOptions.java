package ch.uniport.gateway.proxy.middleware.oauth2;

import ch.uniport.gateway.core.config.model.ModelStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = OAuth2RegistrationMiddlewareOptions.Builder.class)
public abstract class AbstractOAuth2RegistrationMiddlewareOptions extends AbstractOAuth2MiddlewareOptionsBase {

}
