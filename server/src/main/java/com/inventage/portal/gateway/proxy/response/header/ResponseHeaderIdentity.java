package com.inventage.portal.gateway.proxy.response.header;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class ResponseHeaderIdentity implements ResponseHeaderMiddleware<RoutingContext, MultiMap> {

    @Override
    public MultiMap apply(RoutingContext routingContext, MultiMap headers) {
        return headers;
    }
}
