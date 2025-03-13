package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import io.micrometer.common.lang.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ControlApiMiddlewareOptions.Builder.class)
public abstract class AbstractControlApiMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddlewareOptions.class);

    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_ACTION)
    public abstract ControlApiAction getAction();

    @Default
    @Nullable
    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_SESSION_RESET_URL)
    public String getSessionResetURL() {
        logDefault(LOGGER, ControlApiMiddlewareFactory.CONTROL_API_SESSION_RESET_URL, ControlApiMiddlewareFactory.DEFAULT_RESET_URL);
        return ControlApiMiddlewareFactory.DEFAULT_RESET_URL;
    }
}
