package ch.uniport.gateway.proxy.middleware.oauth2;

import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = OAuth2MiddlewareOptions.Builder.class)
public abstract class AbstractOAuth2MiddlewareOptions extends AbstractOAuth2MiddlewareOptionsBase {

}
