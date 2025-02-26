package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.deserialize.GatewayMiddlewareJsonDeserializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = GatewayMiddlewareJsonDeserializer.class)
public class GatewayMiddleware {

    private String name;
    private String type;
    private GatewayMiddlewareOptions options;

    public GatewayMiddleware() {
    }

    public GatewayMiddleware(String name, String type, GatewayMiddlewareOptions options) {
        this.name = name;
        this.type = type;
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public GatewayMiddlewareOptions getOptions() {
        return options.clone();
    }

}
