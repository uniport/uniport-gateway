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
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public String apply(String uri) {
        if (!this.pattern.matcher(uri).matches()) {
            LOGGER.debug("apply: Skipping path replacement of non matching URI '{}'", uri);
            return uri;
        }
        String newURI = this.pattern.matcher(uri).replaceAll(this.replacement);

        LOGGER.debug("apply: replace path '{}' with '{}", uri, newURI);
        return newURI;
    }
}
