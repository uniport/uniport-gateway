package com.inventage.portal.gateway.core.middleware.redirectRegex;

import io.vertx.ext.web.RoutingContext;

public class RedirectPathHandlerImpl implements RedirectPathHandler {

    private final String destination;

    public RedirectPathHandlerImpl(String destination) {
        this.destination = destination;
    }

    @Override
    public void handle(RoutingContext ctx) {
        ctx.redirect(this.destination);
    }
}
