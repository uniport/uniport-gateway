package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.listener.Listener;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelBuilderListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelBuilderListener.class);

    private static final String NAME = "ModelBuilderListener";

    @Override
    public void listen(JsonObject config) {
        final JsonObject httpJson = config.getJsonObject(DynamicConfiguration.HTTP);
        final ObjectMapper codec = new ObjectMapper();
        Gateway gateway = null;
        try {
            gateway = codec.readValue(httpJson.encode(), Gateway.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        return NAME;
    }

}
