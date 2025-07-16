package ch.uniport.gateway.proxy.middleware.headers;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import java.util.Map.Entry;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// client --> request headers           --> HeaderMiddleware --> modified request headers --> server
// client <-- modified response headers <-- HeaderMiddleware <-- response headers         <-- server

/**
 * Manages request/response headers. It can add/remove headers on both requests
 * and responses.
 */
public class HeaderMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddleware.class);

    private final String name;
    private final MultiMap requestHeaderModifiers;
    private final MultiMap responseHeaderModifiers;

    public HeaderMiddleware(String name, MultiMap requestHeaders, MultiMap responseHeaders) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(requestHeaders, "requestHeaders must not be null");
        Objects.requireNonNull(responseHeaders, "responseHeaders must not be null");

        this.name = name;
        this.requestHeaderModifiers = requestHeaders;
        this.responseHeaderModifiers = responseHeaders;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        modifyHeaders(ctx.request().headers(), requestHeaderModifiers, "request");
        ctx.addHeadersEndHandler(v -> modifyHeaders(ctx.response().headers(), responseHeaderModifiers, "response"));
        ctx.next();
    }

    private void modifyHeaders(MultiMap headers, MultiMap modifiers, String headerType) {
        for (Entry<String, String> header : modifiers.entries()) {
            if (header.getValue().isEmpty()) {
                if (headers.contains(header.getKey())) {
                    LOGGER.debug("Removing {} header '{}'", headerType, header.getKey());
                    headers.remove(header.getKey());
                }
            } else {
                if (!headers.getAll(header.getKey()).contains(header.getValue())) {
                    // only add if not already present
                    LOGGER.debug("Setting {} header '{}:{}'", headerType, header.getKey(), header.getValue());
                    headers.add(header.getKey(), header.getValue());
                }
            }
        }
    }

}
