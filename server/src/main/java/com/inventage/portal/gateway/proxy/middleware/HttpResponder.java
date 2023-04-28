package com.inventage.portal.gateway.proxy.middleware;

import com.inventage.portal.gateway.proxy.middleware.responseSessionCookie.ResponseSessionCookieRemovalMiddleware;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class HttpResponder {

    public static void respondWithRedirectForRetryWithoutSetCookie(RoutingContext ctx) {
        ResponseSessionCookieRemovalMiddleware.addSignal(ctx);
        respondWithRedirectForRetry(ctx);
    }

    public static void respondWithRedirectWithoutSetCookie(String uri, RoutingContext ctx) {
        ResponseSessionCookieRemovalMiddleware.addSignal(ctx);
        respondWithRedirect(uri, ctx);
    }

    public static void respondWithRedirectSameMethodWithoutSetCookie(String uri, RoutingContext ctx) {
        ResponseSessionCookieRemovalMiddleware.addSignal(ctx);
        respondWithRedirect307ForRetry(uri, ctx);
    }

    public static void respondWithRedirectForRetry(RoutingContext ctx) {
        respondWithRedirect307ForRetry(ctx.request().uri(), ctx);
    }

    public static void respondWithRedirect(String uri, RoutingContext ctx) {
        ctx.response()
            .setStatusCode(303) // https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/303
            .putHeader(HttpHeaders.LOCATION, uri)
            .end();
    }

    public static void respondWithRedirect307ForRetry(String uri, RoutingContext ctx) {
        ctx.response()
            .setStatusCode(307) // redirect by using the same HTTP method (307)
            .putHeader(HttpHeaders.LOCATION, uri)
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
            .end("Redirecting for retry to " + ctx.request().uri() + ".");
    }

    public static void respondWithRedirect303ForRetry(String uri, RoutingContext ctx) {
        ctx.response()
            .setStatusCode(303) // redirect by using the same HTTP method (307)
            .putHeader(HttpHeaders.LOCATION, uri)
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
            .end("Redirecting for retry to " + ctx.request().uri() + ".");
    }

    public static void respondWithStatusCode(int statusCode, RoutingContext ctx) {
        ctx.response()
            .setStatusCode(statusCode)
            .end();
    }

    public static void respondWithStatusCode(HttpResponseStatus status, RoutingContext ctx) {
        ctx.response()
            .setStatusCode(status.code())
            .setStatusMessage(status.toString())
            .end();
    }

}
