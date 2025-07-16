package ch.uniport.gateway.proxy.middleware.log;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.vertx.core.json.JsonArray;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = RequestResponseLoggerMiddlewareOptions.Builder.class)
public abstract class AbstractRequestResponseLoggerMiddlewareOptions implements MiddlewareOptionsModel {

    public static final JsonArray DEFAULT_CONTENT_TYPES_TO_LOG = JsonArray.of();
    public static final boolean DEFAULT_LOGGING_REQUEST_ENABLED = true;
    public static final boolean DEFAULT_LOGGING_RESPONSE_ENABLED = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddlewareOptions.class);

    @Nullable
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.FILTER_REGEX)
    public abstract String getFilterRegex();

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.CONTENT_TYPES)
    public abstract List<String> getContentTypes();

    @Default
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.LOGGING_REQUEST_ENABLED)
    public boolean isRequestEnabled() {
        logDefault(LOGGER, RequestResponseLoggerMiddlewareFactory.LOGGING_REQUEST_ENABLED,
            DEFAULT_LOGGING_REQUEST_ENABLED);
        return DEFAULT_LOGGING_REQUEST_ENABLED;
    }

    @Default
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.LOGGING_RESPONSE_ENABLED)
    public boolean isResponseEnabled() {
        logDefault(LOGGER, RequestResponseLoggerMiddlewareFactory.LOGGING_RESPONSE_ENABLED,
            DEFAULT_LOGGING_RESPONSE_ENABLED);
        return DEFAULT_LOGGING_RESPONSE_ENABLED;
    }
}
