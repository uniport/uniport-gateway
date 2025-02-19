package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redirecting the client to a different location using regex matching and replacement.
 */
public class RedirectRegexMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddleware.class);

    private final String name;
    private final Pattern pattern;
    private final String replacement;

    public RedirectRegexMiddleware(String name, String regex, String replacement) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(regex, "regex must not be null");
        Objects.requireNonNull(replacement, "replacement must not be null");

        this.name = name;
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        final String oldURI = ctx.request().uri();

        // If the Regexp doesn't match, skip to the next handler.
        if (!this.pattern.matcher(oldURI).matches()) {
            LOGGER.debug("'{}' is skipping redirect of non matching URI '{}'", name, oldURI);
            ctx.next();
            return;
        }

        final String newURI = this.pattern.matcher(oldURI).replaceAll(this.replacement);

        LOGGER.debug("'{}' is redirecting from '{}' to '{}'", name, oldURI, newURI);
        ctx.redirect(newURI);
    }
}
