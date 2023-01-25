package com.inventage.portal.gateway.proxy.middleware;

import com.inventage.portal.gateway.proxy.middleware.responseSessionCookie.ResponseSessionCookieRemovalMiddleware;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class HttpResponder {

    public static void respondWithRedirectWithoutSetCookie(RoutingContext ctx) {
        ResponseSessionCookieRemovalMiddleware.addSignal(ctx);
        respondWithRedirectForRetry(ctx);
    }
    public static void respondWithRedirectForRetry(RoutingContext ctx) {
        ctx.response()
                .setStatusCode(307) // redirect by using the same HTTP method (307)
                .putHeader(HttpHeaders.LOCATION, ctx.request().uri())
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
                .end("Redirecting for retry to " + ctx.request().uri() + ".");
    }

    public static void respondWithStatusCode(int statusCode, RoutingContext ctx) {
        ctx.response()
                .setStatusCode(statusCode)
                .end();
    }

}
