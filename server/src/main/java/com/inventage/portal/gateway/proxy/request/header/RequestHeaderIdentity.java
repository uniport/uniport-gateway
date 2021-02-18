package com.inventage.portal.gateway.proxy.request.header;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class RequestHeaderIdentity implements RequestHeaderMiddleware<RoutingContext, MultiMap> {

    @Override
    public MultiMap apply(RoutingContext routingContext, MultiMap headers) {
        return headers;
    }
}
