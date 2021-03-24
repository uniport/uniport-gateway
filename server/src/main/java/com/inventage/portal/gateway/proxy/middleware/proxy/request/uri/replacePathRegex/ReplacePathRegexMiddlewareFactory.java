package com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.replacePathRegex;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddleware;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddlewareFactory;
import io.vertx.core.json.JsonObject;

/**
 *
 */
public class ReplacePathRegexMiddlewareFactory implements UriMiddlewareFactory {

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX;
    }

    @Override
    public UriMiddleware create(JsonObject uriMiddlewareConfig) {
        return new ReplacePathRegexMiddleware(
                uriMiddlewareConfig
                        .getString(DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX_REGEX),
                uriMiddlewareConfig
                        .getString(DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT));
    }
}
