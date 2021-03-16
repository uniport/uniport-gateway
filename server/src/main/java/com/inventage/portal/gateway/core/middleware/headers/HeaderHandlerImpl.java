package com.inventage.portal.gateway.core.middleware.headers;

import io.vertx.ext.web.RoutingContext;

public class HeaderHandlerImpl implements HeaderHandler {

    public HeaderHandlerImpl() {
    }

    @Override
    public void handle(RoutingContext ctx) {
        System.out.println("Hello from header handler");

        // REMARK: proof of concept, not final
        ctx.request().headers().add("X-FRAME-OPTIONS", "SAMEORIGIN");
        ctx.response().putHeader("X-FRAME-OPTIONS", "DENY");

        ctx.next();
    }
}
