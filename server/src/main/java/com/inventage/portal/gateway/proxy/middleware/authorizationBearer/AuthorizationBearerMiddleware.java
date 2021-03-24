package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class AuthorizationBearerMiddleware implements Middleware {

    private final static String BEARER = "Bearer ";
    private final static String ID_TOKEN = "id_token";
    private final static String ACCESS_TOKEN = "access_token";

    private String sessionScope;

    public AuthorizationBearerMiddleware(String sessionScope) {
        this.sessionScope = sessionScope;
    }

    @Override
    public void handle(RoutingContext ctx) {
        // TODO either put id or access token into header
        if (ctx.user() != null && ctx.user().principal() != null) {
            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, new StringBuilder(BEARER)
                    .append(ctx.user().principal().getString(ACCESS_TOKEN)));

            ctx.response().headers().remove(HttpHeaders.AUTHORIZATION);
        }

        ctx.next();
    }

}
