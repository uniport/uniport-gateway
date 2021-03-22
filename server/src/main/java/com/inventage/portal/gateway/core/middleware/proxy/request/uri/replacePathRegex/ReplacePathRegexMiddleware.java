package com.inventage.portal.gateway.core.middleware.proxy.request.uri.replacePathRegex;

import java.util.regex.Pattern;
import com.inventage.portal.gateway.core.middleware.proxy.request.uri.UriMiddleware;

public class ReplacePathRegexMiddleware implements UriMiddleware {

    private final Pattern pattern;
    private final String replacement;

    public ReplacePathRegexMiddleware(String regex, String replacement) {
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public String apply(String uri) {
        return this.pattern.matcher(uri).replaceAll(this.replacement);
    }
}
