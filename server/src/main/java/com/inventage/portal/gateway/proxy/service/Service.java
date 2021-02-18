package com.inventage.portal.gateway.proxy.service;

import io.vertx.httpproxy.HttpProxy;

public interface Service {

    HttpProxy proxy();
}
