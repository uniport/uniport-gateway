package ch.uniport.gateway.proxy.middleware.headers;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// client --> request headers           --> HeaderMiddleware --> modified request headers --> server
// client <-- modified response headers <-- HeaderMiddleware <-- response headers         <-- server

/**
 * Manages request/response headers. An empty value removes the header.
 * Configured request headers replace any value sent by the client, so a client
 * cannot override them. Configured response headers are added unless the same
 * value is already present.
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

        replaceRequestHeaders(ctx.request().headers(), requestHeaderModifiers);
        ctx.addHeadersEndHandler(v -> appendResponseHeaders(ctx.response().headers(), responseHeaderModifiers));
        ctx.next();
    }

    private void replaceRequestHeaders(MultiMap headers, MultiMap modifiers) {
        for (String headerName : modifiers.names()) {
            final List<String> values = modifiers.getAll(headerName).stream()
                .filter(value -> !value.isEmpty())
                .toList();
            if (values.isEmpty()) {
                LOGGER.debug("Removing request header '{}'", headerName);
                headers.remove(headerName);
            } else {
                LOGGER.debug("Setting request header '{}:{}'", headerName, values);
                headers.set(headerName, values);
            }
        }
    }

    private void appendResponseHeaders(MultiMap headers, MultiMap modifiers) {
        for (Entry<String, String> header : modifiers.entries()) {
            if (header.getValue().isEmpty()) {
                if (headers.contains(header.getKey())) {
                    LOGGER.debug("Removing response header '{}'", header.getKey());
                    headers.remove(header.getKey());
                }
            } else {
                if (!headers.getAll(header.getKey()).contains(header.getValue())) {
                    // only add if not already present
                    LOGGER.debug("Adding response header '{}:{}'", header.getKey(), header.getValue());
                    headers.add(header.getKey(), header.getValue());
                }
            }
        }
    }

}
