package ch.uniport.gateway.proxy.config.model.deserialize;

import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import ch.uniport.gateway.proxy.config.model.MiddlewareModel;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactoryLoader;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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

        final Optional<MiddlewareFactory> middlewareFactory = MiddlewareFactoryLoader.getFactory(type);
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
