package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import io.micrometer.common.lang.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ControlApiMiddlewareOptions.Builder.class)
public abstract class AbstractControlApiMiddlewareOptions implements MiddlewareOptionsModel {

    // defaults
    public static final String DEFAULT_RESET_URL = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddlewareOptions.class);

    @JsonProperty(ControlApiMiddlewareFactory.ACTION)
    public abstract ControlApiAction getAction();

    @Default
    @Nullable
    @JsonProperty(ControlApiMiddlewareFactory.SESSION_RESET_URL)
    public String getSessionResetURL() {
        logDefault(LOGGER, ControlApiMiddlewareFactory.SESSION_RESET_URL, DEFAULT_RESET_URL);
        return DEFAULT_RESET_URL;
    }
}
