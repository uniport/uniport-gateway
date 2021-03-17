package com.inventage.portal.gateway.core.middleware.authorizationBearer;

import io.vertx.ext.web.RoutingContext;
import io.vertx.core.http.HttpHeaders;

public class AuthorizationBearerHandlerImpl implements AuthorizationBearerHandler {

    private final static String BEARER = "Bearer ";
    private final static String ACCESS_TOKEN = "access_token";

    public AuthorizationBearerHandlerImpl() {
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.user() != null && ctx.user().principal() != null) {
            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, new StringBuilder(BEARER)
                    .append(ctx.user().principal().getString(ACCESS_TOKEN)));

            ctx.response().headers().remove(HttpHeaders.AUTHORIZATION);
        }

        ctx.next();
    }
}
