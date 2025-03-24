package com.inventage.portal.gateway.proxy.model.deserialize;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddleware;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.io.IOException;
import java.util.Optional;

public class GatewayMiddlewareJsonDeserializer extends JsonDeserializer<GatewayMiddleware> {

    @Override
    public GatewayMiddleware deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        final GatewayMiddleware.Builder builder = GatewayMiddleware.builder();

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
        final GatewayMiddlewareOptions options = codec.treeToValue(optionsNode, middlewareFactory.get().modelType());
        builder.withOptions(options);

        return builder.build();
    }

}
