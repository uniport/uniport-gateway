package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaimToHeaderMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ClaimToHeaderMiddlewareFactory.CLAIM_TO_HEADER_NAME)
    private String name;

    @JsonProperty(ClaimToHeaderMiddlewareFactory.CLAIM_TO_HEADER_PATH)
    private String path;

    public ClaimToHeaderMiddlewareOptions() {
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public ClaimToHeaderMiddlewareOptions clone() {
        try {
            return (ClaimToHeaderMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
