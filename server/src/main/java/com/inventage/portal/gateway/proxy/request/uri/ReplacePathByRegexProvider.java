package com.inventage.portal.gateway.proxy.request.uri;

import io.vertx.core.json.JsonObject;

/**
 *
 */
public class ReplacePathByRegexProvider implements UriMiddlewareProvider {

    public static final String REGEX = "regex";
    public static final String REPLACEMENT = "replacement";

    @Override
    public String provides() {
        return ReplacePathByRegex.class.getSimpleName();
    }

    @Override
    public UriMiddleware create(JsonObject uriMiddlewareConfig) {
        return new ReplacePathByRegex(uriMiddlewareConfig.getString(REGEX),
                uriMiddlewareConfig.getString(REPLACEMENT));
    }
}
