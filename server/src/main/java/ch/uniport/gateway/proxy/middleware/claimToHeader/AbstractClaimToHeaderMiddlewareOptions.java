package ch.uniport.gateway.proxy.middleware.claimToHeader;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ClaimToHeaderMiddlewareOptions.Builder.class)
public abstract class AbstractClaimToHeaderMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(ClaimToHeaderMiddlewareFactory.NAME)
    public abstract String getName();

    @JsonProperty(ClaimToHeaderMiddlewareFactory.PATH)
    public abstract String getPath();
}