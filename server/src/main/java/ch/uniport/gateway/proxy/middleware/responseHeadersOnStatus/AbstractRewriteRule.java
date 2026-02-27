package ch.uniport.gateway.proxy.middleware.responseHeadersOnStatus;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = RewriteRule.Builder.class)
public abstract class AbstractRewriteRule implements MiddlewareOptionsModel {

    @JsonProperty("regex")
    public abstract String getRegex();

    @JsonProperty("replacement")
    public abstract String getReplacement();
}
