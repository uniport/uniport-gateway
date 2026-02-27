package ch.uniport.gateway.proxy.middleware.responseHeadersOnStatus;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modifies response headers only when the response status code matches the configured value.
 * Supports both full replacement (set) and regex-based rewriting of header values.
 */
public class ResponseHeadersOnStatusMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHeadersOnStatusMiddleware.class);

    private final String name;
    private final int statusCode;
    private final MultiMap setHeaders;
    private final Map<String, CompiledRewriteRule> rewriteRules;

    public ResponseHeadersOnStatusMiddleware(
        String name, int statusCode, MultiMap setHeaders, Map<String, CompiledRewriteRule> rewriteRules
    ) {
        Objects.requireNonNull(name, "name must not be null");

        this.name = name;
        this.statusCode = statusCode;
        this.setHeaders = setHeaders == null ? null : MultiMap.caseInsensitiveMultiMap().addAll(setHeaders);
        this.rewriteRules = rewriteRules == null ? null : Map.copyOf(rewriteRules);
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        ctx.addHeadersEndHandler(v -> {
            if (ctx.response().getStatusCode() == statusCode) {
                // 1. Rewrite existing header values
                if (rewriteRules != null) {
                    for (Map.Entry<String, CompiledRewriteRule> entry : rewriteRules.entrySet()) {
                        final String headerName = entry.getKey();
                        final CompiledRewriteRule rule = entry.getValue();
                        final String current = ctx.response().headers().get(headerName);
                        if (current != null) {
                            final String rewritten = rule.pattern().matcher(current).replaceAll(rule.replacement());
                            LOGGER.debug("{}: Rewrote header '{}'", name, headerName);
                            LOGGER.trace("{}: Rewrote header '{}': '{}' -> '{}'", name, headerName, current, rewritten);
                            ctx.response().headers().set(headerName, rewritten);
                        } else {
                            LOGGER.trace("{}: Header '{}' not present in response, skipping rewrite", name, headerName);
                        }
                    }
                }
                // 2. Set (replace) headers
                if (setHeaders != null) {
                    for (Map.Entry<String, String> entry : setHeaders.entries()) {
                        LOGGER.debug("{}: Set header '{}'", name, entry.getKey());
                        LOGGER.trace("{}: Set header '{}' to '{}'", name, entry.getKey(), entry.getValue());
                        ctx.response().headers().set(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                LOGGER.trace("{}: Status code {} does not match configured {}, skipping",
                    name, ctx.response().getStatusCode(), statusCode);
            }
        });
        ctx.next();
    }

    /**
     * Holds a compiled regex pattern and its replacement string.
     */
    public record CompiledRewriteRule(Pattern pattern, String replacement) {

        public CompiledRewriteRule {
            Objects.requireNonNull(pattern, "pattern must not be null");
            Objects.requireNonNull(replacement, "replacement must not be null");
        }
    }
}
