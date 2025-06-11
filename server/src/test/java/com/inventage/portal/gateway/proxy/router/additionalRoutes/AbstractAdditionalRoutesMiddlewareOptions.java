package com.inventage.portal.gateway.proxy.router.additionalRoutes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = AdditionalRoutesMiddlewareOptions.Builder.class)
public abstract class AbstractAdditionalRoutesMiddlewareOptions implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalRoutesMiddlewareOptions.class);

    @Default
    @JsonProperty(AdditionalRoutesMiddlewareFactory.PATH)
    public String getPath() {
        logDefault(LOGGER, AdditionalRoutesMiddlewareFactory.PATH, AdditionalRoutesMiddlewareFactory.DEFAULT_ADDITIONAL_ROUTES_PATH);
        return AdditionalRoutesMiddlewareFactory.DEFAULT_ADDITIONAL_ROUTES_PATH;
    }

}
