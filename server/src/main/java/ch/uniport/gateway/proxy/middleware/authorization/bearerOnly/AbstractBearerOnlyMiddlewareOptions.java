package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly;

import ch.uniport.gateway.proxy.middleware.ModelStyle;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = BearerOnlyMiddlewareOptions.Builder.class)
public abstract class AbstractBearerOnlyMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BearerOnlyMiddlewareOptions.class);

    public static final boolean DEFAULT_OPTIONAL = false;

    @Default
    @JsonProperty(BearerOnlyMiddlewareFactory.OPTIONAL)
    public boolean isOptional() {
        logDefault(LOGGER, BearerOnlyMiddlewareFactory.OPTIONAL, DEFAULT_OPTIONAL);
        return DEFAULT_OPTIONAL;
    }
}
