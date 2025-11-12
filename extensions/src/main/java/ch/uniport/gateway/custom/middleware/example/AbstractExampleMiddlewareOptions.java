package ch.uniport.gateway.custom.middleware.example;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ExampleMiddlewareOptions.Builder.class)
public abstract class AbstractExampleMiddlewareOptions implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleMiddlewareOptions.class);

    public static final String DEFAULT_HEADER_KEY = "x-uniport-gateway";

    @Default
    @JsonProperty(ExampleMiddlewareFactory.HEADER_KEY)
    public String getHeaderKey() {
        logDefault(LOGGER, ExampleMiddlewareFactory.HEADER_KEY, DEFAULT_HEADER_KEY);
        return DEFAULT_HEADER_KEY;
    }

    @JsonProperty(ExampleMiddlewareFactory.HEADER_VALUE)
    public abstract String getHeaderValue();
}
