package com.inventage.portal.gateway.proxy.request.header;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * RequestHeaderMiddleware for setting the HTTP header 'X-Forwarded-Host'.
 *
 */
public class ForwardedHostHeader implements RequestHeaderMiddleware<RoutingContext, MultiMap> {

    public static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    @Override
    public MultiMap apply(RoutingContext routingContext, MultiMap headers) {
        if (!headers.contains(X_FORWARDED_HOST)) {
            return headers.add(X_FORWARDED_HOST,
                    String.format("%s", routingContext.request().host()));
        }
        return headers;
    }
}
