package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;

/**
 * The incoming requests can be prepared by different middlewares before being routed to its final
 * destination. Every one has to implement the io.vertx.core.Handler interface.
 */
public interface Middleware extends Handler<RoutingContext> {
    String MODIFIERS_PREFIX = "portal-gateway-middleware";
    String REQUEST_URI_MODIFIERS = String.format("%s-request-uri-modifiers", MODIFIERS_PREFIX);

    // Modifier type used by ProxiedHttpServerResponse
    String RESPONSE_HEADERS_MODIFIERS = String.format("%s-response-headers-modifiers", MODIFIERS_PREFIX);

    // addModifier allows to modify the request/response to/returned by forwarded servers.
    default void addModifier(RoutingContext ctx, Handler modifier, String modifierType) {
        List<Handler> modifiers = ctx.get(modifierType);
        if (modifiers == null) {
            modifiers = new ArrayList<>();
        }
        modifiers.add(modifier);
        ctx.put(modifierType, modifiers);
    }

    /**
     * @param ctx
     *            current request context
     * @param modifier
     *            to be applied when processing incoming response headers
     */
    default void addResponseHeaderModifier(RoutingContext ctx, Handler modifier) {
        addModifier(ctx, modifier, RESPONSE_HEADERS_MODIFIERS);
    }
}
