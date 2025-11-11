package ch.uniport.gateway.proxy.middleware.redirectRegex;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
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
