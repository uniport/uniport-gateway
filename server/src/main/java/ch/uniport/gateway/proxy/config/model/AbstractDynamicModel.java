package ch.uniport.gateway.proxy.config.model;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = DynamicModel.Builder.class)
public abstract class AbstractDynamicModel {

    @JsonProperty(DynamicConfiguration.ROUTERS)
    public abstract List<RouterModel> getRouters();

    @JsonProperty(DynamicConfiguration.MIDDLEWARES)
    public abstract List<MiddlewareModel> getMiddlewares();

    @JsonProperty(DynamicConfiguration.SERVICES)
    public abstract List<ServiceModel> getServices();
}
