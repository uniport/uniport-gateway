package ch.uniport.gateway.proxy.middleware.responseHeadersOnStatus;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ResponseHeadersOnStatusMiddlewareOptions.Builder.class)
public abstract class AbstractResponseHeadersOnStatusMiddlewareOptions implements MiddlewareOptionsModel {

    @JsonProperty(ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE)
    public abstract int getStatusCode();

    @Nullable
    @JsonProperty(ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS)
    public abstract Map<String, String> getSetResponseHeaders();

    @Nullable
    @JsonProperty(ResponseHeadersOnStatusMiddlewareFactory.REWRITE_RESPONSE_HEADERS)
    public abstract Map<String, RewriteRule> getRewriteResponseHeaders();
}
