package com.inventage.portal.gateway.core.middleware.replacepathregex;

import java.util.regex.Pattern;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class ReplacePathRegexHandlerImpl implements ReplacePathRegexHandler {

    private final Pattern pattern;
    private final String replacement;

    public ReplacePathRegexHandlerImpl(String regex, String replacement) {
        this.pattern = Pattern.compile(regex);
        this.replacement = replacement;
    }

    @Override
    public void handle(RoutingContext ctx) {
        // TODO: direct uri manipultation is not possible
        ctx.next();
    }
}
