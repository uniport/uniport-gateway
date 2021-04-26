package com.inventage.portal.gateway.proxy.middleware;

import java.util.ArrayList;
import java.util.List;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * The incoming requests can be prepared by different middlewares before being routed to its final
 * destination. Every one has to implement this interface.
 */
public interface Middleware extends Handler<RoutingContext> {
    public final String RESPONSE_HEADERS_MODIFIERS =
            "portal-gateway-middleware-response-headers-modifiers";

    // addResponseHeadersModifier allows to modify the response returned by the microservices.
    default void addResponseHeadersModifier(RoutingContext ctx, Handler<MultiMap> modifier) {
        List<Handler<MultiMap>> modifiers = ctx.get(RESPONSE_HEADERS_MODIFIERS);
        if (modifiers == null) {
            modifiers = new ArrayList<Handler<MultiMap>>();
        }
        modifiers.add(modifier);
        ctx.put(RESPONSE_HEADERS_MODIFIERS, modifiers);
    }
}
