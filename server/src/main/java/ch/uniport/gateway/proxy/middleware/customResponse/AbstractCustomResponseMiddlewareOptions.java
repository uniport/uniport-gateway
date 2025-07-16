package ch.uniport.gateway.proxy.middleware.customResponse;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CustomResponseMiddlewareOptions.Builder.class)
public abstract class AbstractCustomResponseMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(CustomResponseMiddlewareFactory.STATUS_CODE)
    public abstract int getStatusCode();

    @JsonProperty(CustomResponseMiddlewareFactory.HEADERS)
    public abstract Map<String, String> getHeaders();

    @JsonProperty(CustomResponseMiddlewareFactory.CONTENT)
    public abstract String getContent();
}
