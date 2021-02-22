package com.inventage.portal.gateway.proxy.service;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.httpproxy.HttpProxy;

/**
 *
 */
public class ServiceJsonFile implements Service {

    final String name;
    final String serverHost;
    final int serverPort;
    final HttpProxy httpProxy;


    public ServiceJsonFile(String serviceName, String serverHost, int serverPort, HttpProxy httpProxy) {
        this.name = serviceName;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.httpProxy = httpProxy;
        httpProxy.target(serverPort, serverHost);
    }

    public String toString() {
        return String.format("%s at %s:%d", name, serverHost, serverPort);
    }

    @Override
    public void handle(HttpServerRequest outboundRequest) {
        httpProxy.handle(outboundRequest);
    }
}
