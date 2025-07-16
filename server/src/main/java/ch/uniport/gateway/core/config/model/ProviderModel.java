package ch.uniport.gateway.core.config.model;

import ch.uniport.gateway.core.config.model.deserialize.ProviderModelJsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ProviderModelJsonDeserializer.class)
public interface ProviderModel {

    String getName();

}
