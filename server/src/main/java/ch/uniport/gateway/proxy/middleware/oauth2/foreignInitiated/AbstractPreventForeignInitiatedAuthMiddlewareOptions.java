package ch.uniport.gateway.proxy.middleware.oauth2.foreignInitiated;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = PreventForeignInitiatedAuthMiddlewareOptions.Builder.class)
public abstract class AbstractPreventForeignInitiatedAuthMiddlewareOptions implements MiddlewareOptionsModel {

    public static final String DEFAULT_REDIRECT_URI = "/";

    private static final Logger LOGGER = LoggerFactory.getLogger(PreventForeignInitiatedAuthMiddlewareOptions.class);

    @Default
    @JsonProperty(PreventForeignInitiatedAuthMiddlewareFactory.REDIRECT_URI)
    public String getRedirectURI() {
        logDefault(LOGGER, PreventForeignInitiatedAuthMiddlewareFactory.REDIRECT_URI, DEFAULT_REDIRECT_URI);
        return DEFAULT_REDIRECT_URI;
    }

}
