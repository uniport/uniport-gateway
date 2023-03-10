package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Replaces the URI using regex matching and replacement.
 */
public class ReplacePathRegexMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacePathRegexMiddleware.class);

    private final String name;
    private final Pattern pattern;
    private final String replacement;

    public ReplacePathRegexMiddleware(String name, String regex, String replacement) {
        this.name = name;
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        final Handler<StringBuilder> reqUriModifier = uri -> {
            uri.replace(0, uri.length(), apply(uri.toString()));
        };
        this.addModifier(ctx, reqUriModifier, Middleware.REQUEST_URI_MODIFIERS);

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
