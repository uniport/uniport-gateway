package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import io.micrometer.common.lang.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = ControlApiMiddlewareOptions.Builder.class)
public abstract class AbstractControlApiMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_ACTION)
    public abstract ControlApiAction getAction();

    @Default
    @Nullable
    @JsonProperty(ControlApiMiddlewareFactory.CONTROL_API_SESSION_RESET_URL)
    public String getSessionResetURL() {
        return ControlApiMiddlewareFactory.DEFAULT_RESET_URL;
    }
}
