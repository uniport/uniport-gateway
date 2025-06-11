package com.inventage.portal.gateway.proxy.middleware.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddlewareOptions.class);

    @Nullable
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.FILTER_REGEX)
    public abstract String getFilterRegex();

    @JsonProperty(RequestResponseLoggerMiddlewareFactory.CONTENT_TYPES)
    public abstract List<String> getContentTypes();

    @Default
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.LOGGING_REQUEST_ENABLED)
    public boolean isRequestEnabled() {
        logDefault(LOGGER, RequestResponseLoggerMiddlewareFactory.LOGGING_REQUEST_ENABLED, RequestResponseLoggerMiddlewareFactory.DEFAULT_LOGGING_REQUEST_ENABLED);
        return RequestResponseLoggerMiddlewareFactory.DEFAULT_LOGGING_REQUEST_ENABLED;
    }

    @Default
    @JsonProperty(RequestResponseLoggerMiddlewareFactory.LOGGING_RESPONSE_ENABLED)
    public boolean isResponseEnabled() {
        logDefault(LOGGER, RequestResponseLoggerMiddlewareFactory.LOGGING_RESPONSE_ENABLED, RequestResponseLoggerMiddlewareFactory.DEFAULT_LOGGING_RESPONSE_ENABLED);
        return RequestResponseLoggerMiddlewareFactory.DEFAULT_LOGGING_RESPONSE_ENABLED;
    }
}
