package com.inventage.portal.gateway.core.config.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.deserialize.ProviderModelJsonDeserializer;

@JsonDeserialize(using = ProviderModelJsonDeserializer.class)
public interface ProviderModel {

    String getName();

}
