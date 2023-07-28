package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
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
    String RESPONSE_HEADERS_MODIFIERS = String.format("%s-response-headers-modifiers", MODIFIERS_PREFIX);

    /**
     * @param ctx
     *            current request context
     * @param modifier
     *            to be applied when processing the incoming request URI
     */
    default void addRequestURIModifier(RoutingContext ctx, Handler<StringBuilder> modifier) {
        appendModifier(ctx, modifier, REQUEST_URI_MODIFIERS);
    }

    /**
     * @param ctx
     *            current request context
     * @param modifier
     *            to be applied when processing incoming response headers
     */
    default void addResponseHeaderModifier(RoutingContext ctx, Handler<MultiMap> modifier) {
        prependModifier(ctx, modifier, RESPONSE_HEADERS_MODIFIERS);
    }

    // prependModifier allows to modify the response returned by forwarded servers.
    private void prependModifier(RoutingContext ctx, Handler modifier, String modifierType) {
        addModifier(ctx, modifier, modifierType, true);
    }

    // appendModifier allows to modify the request to by forwarded servers.
    private void appendModifier(RoutingContext ctx, Handler modifier, String modifierType) {
        addModifier(ctx, modifier, modifierType, false);
    }

    private void addModifier(RoutingContext ctx, Handler modifier, String modifierType, boolean prepend) {
        List<Handler> modifiers = ctx.get(modifierType);
        if (modifiers == null) {
            modifiers = new ArrayList<>();
        }
        if (prepend) {
            modifiers.add(0, modifier);
        } else {
            modifiers.add(modifier);
        }
        ctx.put(modifierType, modifiers);
    }
}
