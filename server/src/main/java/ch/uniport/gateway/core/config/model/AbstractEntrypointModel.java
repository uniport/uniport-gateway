package ch.uniport.gateway.core.config.model;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.proxy.config.model.MiddlewareModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = EntrypointModel.Builder.class)
public abstract class AbstractEntrypointModel {

    @JsonProperty(StaticConfiguration.ENTRYPOINT_NAME)
    public abstract String getName();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_PORT)
    public abstract int getPort();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_MIDDLEWARES)
    public abstract List<MiddlewareModel> getMiddlewares();

}
