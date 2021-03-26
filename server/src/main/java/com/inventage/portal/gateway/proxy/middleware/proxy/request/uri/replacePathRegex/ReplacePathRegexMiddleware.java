package com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.replacePathRegex;

import java.util.regex.Pattern;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces the URI using regex matching and replacement.
 */
public class ReplacePathRegexMiddleware implements UriMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacePathRegexMiddleware.class);

    private final Pattern pattern;
    private final String replacement;

    public ReplacePathRegexMiddleware(String regex, String replacement) {
        LOGGER.trace("construcutor");
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public String apply(String uri) {
        LOGGER.trace("apply");
        return this.pattern.matcher(uri).replaceAll(this.replacement);
    }
}
