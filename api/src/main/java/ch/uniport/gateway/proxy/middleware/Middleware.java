package ch.uniport.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The incoming requests can be prepared by different middlewares before being
 * routed to its final
 * destination. Every one has to implement the io.vertx.core.Handler interface.
 */
public interface Middleware extends Handler<RoutingContext> {

    Logger LOGGER = LoggerFactory.getLogger(Middleware.class);

    String MODIFIERS_PREFIX = "uniport-gateway-middleware";

    String REQUEST_URI_MODIFIERS = String.format("%s-request-uri-modifiers", MODIFIERS_PREFIX);
    String REQUEST_HEADERS_MODIFIERS = String.format("%s-request-headers-modifiers", MODIFIERS_PREFIX);

    /**
     * Vertx does not allow to change the path of a request in a common handler.
     * So we do it later, before the outgoing request is sent to the backend in the
     * ProxyMiddleware, as it is not important earlier anyway.
     * 
     * @param ctx
     *            current request context
     * @param modifier
     *            to be applied when processing the incoming request URI
     */
    default void addRequestURIModifier(RoutingContext ctx, Handler<StringBuilder> modifier) {
        appendModifier(ctx, modifier, REQUEST_URI_MODIFIERS);
    }

    /**
     * Generally, this should only be used as a last resort to modify the headers of
     * the outgoing request.
     * It is designed for the special case when headers could not have been modified
     * earlier due to logical constraints,
     * but must be modified before the request is sent to the backend, e.g. removing
     * the session cookie from the request.
     * 
     * @param ctx
     *            current request context
     * @param modifier
     *            to be applied when processing the incoming request headers
     *            just before sending it to the backend service
     */
    default void addRequestHeadersModifier(RoutingContext ctx, Handler<MultiMap> modifier) {
        appendModifier(ctx, modifier, REQUEST_HEADERS_MODIFIERS);
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
        LOGGER.debug("Modifier '{}' added to '{}'.", modifier, modifierType);
    }
}
