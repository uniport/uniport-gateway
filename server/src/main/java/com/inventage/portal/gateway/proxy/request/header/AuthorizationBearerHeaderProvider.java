package com.inventage.portal.gateway.proxy.request.header;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AuthorizationBearerHeaderProvider implements RequestHeaderMiddlewareProvider {

    @Override
    public String provides() {
        return AuthorizationBearerHeader.class.getSimpleName();
    }

    @Override
    public RequestHeaderMiddleware<RoutingContext, MultiMap> create(
            JsonObject headerMiddlewareConfig) {
        return new AuthorizationBearerHeader();
    }
}
