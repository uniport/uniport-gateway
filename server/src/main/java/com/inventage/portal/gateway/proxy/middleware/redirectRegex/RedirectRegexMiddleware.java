package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Redirecting the client to a different location using regex matching and replacement.
 */
public class RedirectRegexMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddleware.class);

    private final String name;
    private final Pattern pattern;
    private final String replacement;

    public RedirectRegexMiddleware(String name, String regex, String replacement) {
        this.name = name;
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public void handle(RoutingContext ctx) {
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
