package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.RoutingContext;

/**
 * Remove the session cookie from the response, if the context holds a data for the key `REMOVE_SESSION_COOKIE_SIGNAL`.
 */
public class ResponseSessionCookieMiddleware implements Middleware {

    public static final String REMOVE_SESSION_COOKIE_SIGNAL = "REMOVE_SESSION_COOKIE_SIGNAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieMiddleware.class);

    private final String sessionCookieName;

    public ResponseSessionCookieMiddleware(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    // endHandler execution order: headersEndHandler -> bodyEndHandler -> endHandler

    @Override
    public void handle(RoutingContext ctx) {
        ctx.addHeadersEndHandler(v -> removeSessionCookie(ctx));
        ctx.next();
    }

    protected void removeSessionCookie(RoutingContext ctx) {
        if (ctx.get(REMOVE_SESSION_COOKIE_SIGNAL) != null) {
            LOGGER.debug("With value '{}'", ctx.getCookie(sessionCookieName).getValue());
            // invalidate=false: session cookie should only be removed from response, not unset in client
            ctx.removeCookie(sessionCookieName, false);
        }
    }
}
