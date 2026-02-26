package ch.uniport.gateway.proxy.middleware.cors;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for adding the CorsHandler of Vert.x.
 * see https://vertx.io/docs/vertx-web/java/#_cors_handling
 * see https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
 * see
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin
 * see https://fetch.spec.whatwg.org/#http-cors-protocol
 */
public class CorsMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorsMiddleware.class);

    private final String name;
    private final CorsHandler corsHandler;

    public CorsMiddleware(
        String name,
        List<String> allowedOrigin,
        List<String> allowedOriginPattern,
        Set<HttpMethod> allowedMethods,
        Set<String> allowedHeaders,
        Set<String> exposedHeaders,
        int maxAgeSeconds,
        boolean allowCredentials,
        boolean allowPrivateNetwork
    ) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(allowedOrigin, "allowedOrigin must not be null");
        Objects.requireNonNull(allowedOriginPattern, "allowedOriginPattern must not be null");
        Objects.requireNonNull(allowedMethods, "allowedMethods must not be null");
        Objects.requireNonNull(allowedHeaders, "allowedHeaders must not be null");
        Objects.requireNonNull(exposedHeaders, "exposedHeaders must not be null");

        this.name = name;
        this.corsHandler = CorsHandler.create();

        if (!allowedOrigin.isEmpty()) {
            corsHandler.addOrigins(allowedOrigin);
        }
        if (!allowedOriginPattern.isEmpty()) {
            corsHandler.addOriginsWithRegex(allowedOriginPattern);
        }
        if (!allowedMethods.isEmpty()) {
            corsHandler.allowedMethods(allowedMethods);
        }
        if (!allowedHeaders.isEmpty()) {
            corsHandler.allowedHeaders(allowedHeaders);
        }
        if (!exposedHeaders.isEmpty()) {
            corsHandler.exposedHeaders(exposedHeaders);
        }

        if (maxAgeSeconds >= 0) {
            corsHandler.maxAgeSeconds(maxAgeSeconds);
        }
        corsHandler.allowCredentials(allowCredentials);
        corsHandler.allowPrivateNetwork(allowPrivateNetwork);
    }

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        // Deduplicate CORS headers that may be set by both the CorsHandler and a
        // proxied backend service. The headersEndHandler runs just before the
        // response is sent, after the proxy has copied backend headers.
        ctx.addHeadersEndHandler(v -> {
            final MultiMap headers = ctx.response().headers();
            deduplicateHeader(headers, ACCESS_CONTROL_ALLOW_ORIGIN);
            deduplicateHeader(headers, ACCESS_CONTROL_ALLOW_CREDENTIALS);
            deduplicateHeader(headers, ACCESS_CONTROL_EXPOSE_HEADERS);
        });

        corsHandler.handle(ctx);
    }

    private void deduplicateHeader(MultiMap headers, String headerName) {
        final List<String> values = headers.getAll(headerName);
        if (values.size() > 1) {
            LOGGER.debug("{}: Deduplicating response header '{}' ({} values)", name, headerName, values.size());
            headers.set(headerName, values.get(0));
        }
    }
}
