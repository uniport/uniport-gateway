package ch.uniport.gateway.core.config.model;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = StaticModel.Builder.class)
public abstract class AbstractStaticModel {

    @JsonProperty(StaticConfiguration.ENTRYPOINTS)
    public abstract List<EntrypointModel> getEntrypoints();

    @JsonProperty(StaticConfiguration.PROVIDERS)
    public abstract List<ProviderModel> getProviders();
}
