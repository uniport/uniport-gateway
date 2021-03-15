package com.inventage.portal.gateway.proxy.request.header;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * RequestHeaderMiddleware for filling the HTTP header 'Authorization: Bearer ' with the
 * 'access_token' property from the principal from the user within the routing context.
 */
public class AuthorizationBearerHeader
        implements RequestHeaderMiddleware<RoutingContext, MultiMap> {

    private final static String BEARER = "Bearer ";
    private final static String ACCESS_TOKEN = "access_token";

    @Override
    public MultiMap apply(RoutingContext routingContext, MultiMap headers) {
        if (routingContext.user() != null && routingContext.user().principal() != null)
            headers.add(HttpHeaders.AUTHORIZATION, new StringBuilder(BEARER)
                    .append(routingContext.user().principal().getString(ACCESS_TOKEN)));
        return headers;
    }
}
