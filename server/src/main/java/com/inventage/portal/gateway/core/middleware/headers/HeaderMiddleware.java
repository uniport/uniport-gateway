package com.inventage.portal.gateway.core.middleware.headers;

import com.inventage.portal.gateway.core.middleware.Middleware;
import io.vertx.ext.web.RoutingContext;

public class HeaderMiddleware implements Middleware {

    @Override
    public void handle(RoutingContext ctx) {
        // TODO: proof of concept, not final
        ctx.request().headers().add("X-FRAME-OPTIONS", "SAMEORIGIN");
        ctx.response().putHeader("X-FRAME-OPTIONS", "DENY");

        ctx.next();
    }
}
