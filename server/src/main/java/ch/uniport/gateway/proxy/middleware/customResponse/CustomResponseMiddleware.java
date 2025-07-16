package ch.uniport.gateway.proxy.middleware.customResponse;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The custom response middleware enables you to return a hard-coded response
 * back to the client that made a request.
 */
public class CustomResponseMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomResponseMiddleware.class);

    private final String name;
    private final Integer statusCode;
    private final MultiMap headers;
    private final String content;

    public CustomResponseMiddleware(String name, String content, Integer statusCode, MultiMap headers) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(statusCode, "statusCode must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        // content is allowed to be null

        this.name = name;
        this.statusCode = statusCode;
        this.headers = headers;
        this.content = content;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        for (Map.Entry<String, String> header : this.headers.entries()) {
            LOGGER.debug("Setting response header '{}:{}'", header.getKey(), header.getValue());
            ctx.response().headers().add(header.getKey(), header.getValue());
        }

        ctx.response().setStatusCode(this.statusCode);
        ctx.end(this.content == null ? "" : this.content);
    }
}
