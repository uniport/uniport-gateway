package com.inventage.portal.gateway.core.middleware.authorizationBearer;

import io.vertx.ext.web.RoutingContext;
import com.inventage.portal.gateway.core.middleware.Middleware;
import io.vertx.core.http.HttpHeaders;

public class AuthorizationBearerMiddleware implements Middleware {

    private final static String BEARER = "Bearer ";
    private final static String ACCESS_TOKEN = "access_token";

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.user() != null && ctx.user().principal() != null)

        {
            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, new StringBuilder(BEARER)
                    .append(ctx.user().principal().getString(ACCESS_TOKEN)));

            ctx.response().headers().remove(HttpHeaders.AUTHORIZATION);
        }

        ctx.next();
    }

}
