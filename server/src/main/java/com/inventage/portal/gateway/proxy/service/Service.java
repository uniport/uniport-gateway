package com.inventage.portal.gateway.proxy.service;


import io.vertx.core.http.HttpServerRequest;
import io.vertx.httpproxy.HttpProxy;

public interface Service {

    void handle(HttpServerRequest outboundRequest);
}
