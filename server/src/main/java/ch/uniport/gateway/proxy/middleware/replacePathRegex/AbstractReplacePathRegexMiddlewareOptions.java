package ch.uniport.gateway.proxy.middleware.replacePathRegex;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ReplacePathRegexMiddlewareOptions.Builder.class)
public abstract class AbstractReplacePathRegexMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REGEX)
    public abstract String getRegex();

    @JsonProperty(ReplacePathRegexMiddlewareFactory.REPLACEMENT)
    public abstract String getReplacement();

}
