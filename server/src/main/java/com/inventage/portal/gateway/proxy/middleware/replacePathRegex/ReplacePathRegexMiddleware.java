package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces the URI using regex matching and replacement.
 */
public class ReplacePathRegexMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacePathRegexMiddleware.class);

    private final String name;
    private final Pattern pattern;
    private final String replacement;

    /**
    */
    public ReplacePathRegexMiddleware(String name, String regex, String replacement) {
        this.name = name;
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        final Handler<StringBuilder> reqUriModifier = uri -> {
            uri.replace(0, uri.length(), apply(uri.toString()));
        };
        this.addRequestURIModifier(ctx, reqUriModifier);

        ctx.next();
    }

    String apply(String uri) {
        if (!this.pattern.matcher(uri).matches()) {
            LOGGER.debug("Skipping path replacement of non matching URI '{}'", uri);
        }
        final String newURI = this.pattern.matcher(uri).replaceAll(this.replacement);

        LOGGER.debug("Replace path '{}' with '{}'", uri, newURI);
        return newURI;
    }
}
