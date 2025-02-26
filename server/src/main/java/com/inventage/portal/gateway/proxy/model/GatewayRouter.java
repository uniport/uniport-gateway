package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayRouter {

    private String name;
    private String rule;
    private Integer priority;
    private List<String> entrypoints;
    private List<String> middlewares; // TODO link?
    private String service; // TODO link?

    public GatewayRouter() {
    }

    public String getName() {
        return name;
    }

    public String getRule() {
        return rule;
    }

    public int gerPriority() {
        return priority;
    }

    public List<String> getEntrypoints() {
        return List.copyOf(entrypoints);
    }

    public List<String> getMiddlewares() {
        return List.copyOf(middlewares);
    }

    public String getService() {
        return service;
    }
}
