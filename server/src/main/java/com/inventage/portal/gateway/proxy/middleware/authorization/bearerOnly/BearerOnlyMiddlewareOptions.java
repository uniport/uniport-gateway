package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BearerOnlyMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @JsonProperty(BearerOnlyMiddlewareFactory.BEARER_ONLY_OPTIONAL)
    private Boolean optional;

    public BearerOnlyMiddlewareOptions() {
    }

    public Boolean isOptional() {
        return optional;
    }

    @Override
    public BearerOnlyMiddlewareOptions clone() {
        final BearerOnlyMiddlewareOptions options = (BearerOnlyMiddlewareOptions) super.clone();
        return options;
    }
}
