package ch.uniport.gateway.core.config.model.deserialize;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.core.config.model.ProviderModel;
import ch.uniport.gateway.proxy.provider.ProviderFactory;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;

public class ProviderModelJsonDeserializer extends JsonDeserializer<ProviderModel> {

    @Override
    public ProviderModel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        final ObjectCodec codec = p.getCodec();
        final JsonNode node = codec.readTree(p);

        final JsonNode nameNode = node.get(StaticConfiguration.PROVIDER_NAME);
        final String name = nameNode.asText();

        final Optional<ProviderFactory> providerFactory = ProviderFactory.Loader.getFactory(name);
        if (providerFactory.isEmpty()) {
            final String errMsg = String.format("Unknown provider '%s'", name);
            throw new IllegalArgumentException(errMsg);
        }

        final ProviderModel provider = codec.treeToValue(node, providerFactory.get().modelType());
        return provider;
    }

}
