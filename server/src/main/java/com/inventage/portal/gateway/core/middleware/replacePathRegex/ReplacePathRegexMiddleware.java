package com.inventage.portal.gateway.core.middleware.replacePathRegex;

import java.util.regex.Pattern;
import com.inventage.portal.gateway.core.middleware.Middleware;
import io.vertx.ext.web.RoutingContext;

public class ReplacePathRegexMiddleware implements Middleware {

    private final Pattern pattern;
    private final String replacement;

    public ReplacePathRegexMiddleware(String regex, String replacement) {
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public void handle(RoutingContext ctx) {
        // TODO/ASK: direct uri manipultation is not possible
        ctx.next();
    }
}
