package com.inventage.portal.gateway.core.middleware.redirectPath;

import com.inventage.portal.gateway.core.middleware.Middleware;
import io.vertx.ext.web.RoutingContext;

public class RedirectPathMiddleware implements Middleware {

    private final String destination;

    public RedirectPathMiddleware(String destination) {
        this.destination = destination;
    }

    @Override
    public void handle(RoutingContext ctx) {
        ctx.redirect(this.destination);
    }
}
