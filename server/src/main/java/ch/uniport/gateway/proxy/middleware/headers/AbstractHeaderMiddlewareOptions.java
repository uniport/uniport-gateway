package ch.uniport.gateway.proxy.middleware.headers;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = HeaderMiddlewareOptions.Builder.class)
public abstract class AbstractHeaderMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_REQUEST)
    public abstract Map<String, Object> getRequestHeaders();

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_RESPONSE)
    public abstract Map<String, Object> getResponseHeaders();
}
