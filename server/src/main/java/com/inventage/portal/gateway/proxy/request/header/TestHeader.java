package com.inventage.portal.gateway.proxy.request.header;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * Request header middleware that adds "PortalGateway" to the HTTP header with a value identifying
 * the running vert.x instance.
 */
public class TestHeader implements RequestHeaderMiddleware<RoutingContext, MultiMap> {

    @Override
    public MultiMap apply(RoutingContext routingContext, MultiMap headers) {
        headers.add("PortalGateway", "" + routingContext.vertx().hashCode());
        return headers;
    }

}
