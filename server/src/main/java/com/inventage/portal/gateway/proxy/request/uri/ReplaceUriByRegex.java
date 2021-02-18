package com.inventage.portal.gateway.proxy.request.uri;

import java.util.regex.Pattern;

public class ReplaceUriByRegex implements UriMiddleware {

    private final Pattern pattern;
    private final String replacement;

    public ReplaceUriByRegex(String regex, String replacement) {
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public String apply(String uri) {
        return pattern.matcher(uri).replaceAll(replacement);
    }
}
