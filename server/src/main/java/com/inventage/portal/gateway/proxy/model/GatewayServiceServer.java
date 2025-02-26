package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayServiceServer {

    String protocol;
    String host;
    Integer port;
    GatewayServiceServerHttpOptions httpsOptions;

    public GatewayServiceServer() {
    }

    public GatewayServiceServer(GatewayServiceServer other) {
        protocol = other.protocol;
        host = other.host;
        port = other.port;
        httpsOptions = new GatewayServiceServerHttpOptions(other.httpsOptions);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public GatewayServiceServerHttpOptions getHttpsOptions() {
        return new GatewayServiceServerHttpOptions(httpsOptions);
    }

}
