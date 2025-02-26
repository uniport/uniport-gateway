package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayService {

    private String name;
    private List<GatewayServiceServer> servers;
    private Boolean verbose;

    public GatewayService() {
    }

    public String getName() {
        return name;
    }

    public List<GatewayServiceServer> getServers() {
        return servers.stream()
            .map(GatewayServiceServer::new)
            .collect(Collectors.toList());
    }

    public Boolean isVerbose() {
        return verbose;
    }

}