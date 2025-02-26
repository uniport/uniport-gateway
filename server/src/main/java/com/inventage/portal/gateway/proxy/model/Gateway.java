package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gateway {

    private List<GatewayRouter> routers;
    private List<GatewayMiddleware> middlewares;
    private List<GatewayService> services;

    public Gateway() {
    }

    public List<GatewayRouter> getRouters() {
        return List.copyOf(routers);
    }

    public List<GatewayMiddleware> getMiddlewares() {
        return List.copyOf(middlewares);
    }

    public List<GatewayService> getServices() {
        return List.copyOf(services);
    }

    @Override
    public String toString() {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }
}
