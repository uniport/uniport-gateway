package ch.uniport.gateway.proxy.config.model;

import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import ch.uniport.gateway.proxy.config.model.deserialize.MiddlewareModelJsonDeserializer;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(using = MiddlewareModelJsonDeserializer.class, builder = MiddlewareModel.Builder.class)
public abstract class AbstractMiddlewareModel {

    @JsonProperty(DynamicConfiguration.MIDDLEWARE_NAME)
    public abstract String getName();

    @JsonProperty(DynamicConfiguration.MIDDLEWARE_TYPE)
    public abstract String getType();

    @Nullable
    @JsonProperty(DynamicConfiguration.MIDDLEWARE_OPTIONS)
    public abstract MiddlewareOptionsModel getOptions();
}
