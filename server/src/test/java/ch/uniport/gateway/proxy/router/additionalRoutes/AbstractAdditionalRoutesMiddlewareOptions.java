package ch.uniport.gateway.proxy.router.additionalRoutes;

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
@JsonDeserialize(builder = AdditionalRoutesMiddlewareOptions.Builder.class)
public abstract class AbstractAdditionalRoutesMiddlewareOptions implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalRoutesMiddlewareOptions.class);

    public static final String DEFAULT_ADDITIONAL_ROUTES_PATH = "/some-path";

    @Default
    @JsonProperty(AdditionalRoutesMiddlewareFactory.PATH)
    public String getPath() {
        logDefault(LOGGER, AdditionalRoutesMiddlewareFactory.PATH, DEFAULT_ADDITIONAL_ROUTES_PATH);
        return DEFAULT_ADDITIONAL_ROUTES_PATH;
    }

}
