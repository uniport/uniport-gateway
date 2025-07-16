package ch.uniport.gateway.proxy.middleware.redirectRegex;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = RedirectRegexMiddlewareOptions.Builder.class)
public abstract class AbstractRedirectRegexMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(RedirectRegexMiddlewareFactory.REGEX)
    public abstract String getRegex();

    @JsonProperty(RedirectRegexMiddlewareFactory.REPLACEMENT)
    public abstract String getReplacement();
}
