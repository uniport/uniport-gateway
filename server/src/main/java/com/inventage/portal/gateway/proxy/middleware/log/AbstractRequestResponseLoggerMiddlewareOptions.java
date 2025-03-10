package com.inventage.portal.gateway.proxy.middleware.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = RequestResponseLoggerMiddlewareOptions.Builder.class)
public abstract class AbstractRequestResponseLoggerMiddlewareOptions implements GatewayMiddlewareOptions {

    @Nullable
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_FILTER_REGEX)
    public abstract String getFilterRegex();

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_CONTENT_TYPES)
    public abstract List<String> getContentTypes();

    @Default
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED)
    public boolean isRequestEnabled() {
        return RequestResponseLoggerMiddlewareFactory.DEFAULT_LOGGING_REQUEST_ENABLED;
    }

    @Default
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED)
    public boolean isResponseEnabled() {
        return RequestResponseLoggerMiddlewareFactory.DEFAULT_LOGGING_RESPONSE_ENABLED;
    }
}
