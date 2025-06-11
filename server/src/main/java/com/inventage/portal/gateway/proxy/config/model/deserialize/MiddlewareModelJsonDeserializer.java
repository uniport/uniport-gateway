package com.inventage.portal.gateway.proxy.config.model.deserialize;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.inventage.portal.gateway.proxy.config.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareModel;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import java.io.IOException;
import java.util.Optional;

public class MiddlewareModelJsonDeserializer extends JsonDeserializer<MiddlewareModel> {

    @Override
    public MiddlewareModel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        final MiddlewareModel.Builder builder = MiddlewareModel.builder();

        final ObjectCodec codec = p.getCodec();
        final JsonNode node = codec.readTree(p);

        final JsonNode nameNode = node.get(DynamicConfiguration.MIDDLEWARE_NAME);
        final String name = nameNode.asText();
        builder.withName(name);

        final JsonNode typeNode = node.get(DynamicConfiguration.MIDDLEWARE_TYPE);
        final String type = typeNode.asText();
        builder.withType(type);

        final Optional<MiddlewareFactory> middlewareFactory = MiddlewareFactory.Loader.getFactory(type);
        if (middlewareFactory.isEmpty()) {
            final String errMsg = String.format("Unknown middleware '%s'", type);
            throw new IllegalArgumentException(errMsg);
        }

        final JsonNode optionsNode;
        if (!node.has(DynamicConfiguration.MIDDLEWARE_OPTIONS)) {
            // empty node: this allows a configuration to not contain an options object
            optionsNode = JsonNodeFactory.instance.objectNode();
        } else {
            optionsNode = node.get(DynamicConfiguration.MIDDLEWARE_OPTIONS);
        }
        final MiddlewareOptionsModel options = codec.treeToValue(optionsNode, middlewareFactory.get().modelType());
        builder.withOptions(options);

        return builder.build();
    }

}
