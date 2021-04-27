package com.inventage.portal.gateway.proxy.middleware;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * The incoming requests can be prepared by different middlewares before being routed to its final
 * destination. Every one has to implement this interface.
 */
public interface Middleware extends Handler<RoutingContext> {
    public final String MODIFIERS_PREFIX = "portal-gateway-middleware";
    public final String REQUEST_URI_MODIFIERS = String.format("%s-request-uri-modifiers", MODIFIERS_PREFIX);
    public final String RESPONSE_HEADERS_MODIFIERS = String.format("%s-response-headers-modifiers", MODIFIERS_PREFIX);

    // addModifier allows to modify the request/response to/returned by forwarded servers.
    default void addModifier(RoutingContext ctx, Handler modifier, String modifierType) {
        List<Handler> modifiers = ctx.get(modifierType);
        if (modifiers == null) {
            modifiers = new ArrayList<Handler>();
        }
        modifiers.add(modifier);
        ctx.put(modifierType, modifiers);
    }
}
