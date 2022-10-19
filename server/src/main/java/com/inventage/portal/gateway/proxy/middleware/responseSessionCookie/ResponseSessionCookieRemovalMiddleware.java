package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.RoutingContext;

/**
 * Remove the session cookie from the response, if the context holds a data for the key `REMOVE_SESSION_COOKIE_SIGNAL`.
 * The `REMOVE_SESSION_COOKIE_SIGNAL` is set in the ReplacedSessionCookieDetection whenever we detect that the user has made a request with
 * a replaced "previously" valid session (replaced by session.regenerateId()). We need to remove the session cookie or else the browser
 * will restore the replaced (now invalid) session cookie. For more details refer to
 * {@link com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddleware}
 *
 * {@link #sessionCookieName} specifies the name (can be configured) of the session cookie. Keep in mind that the name has to be same as
 * {@link com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware#cookieName}.
 *
 */
public class ResponseSessionCookieRemovalMiddleware implements Middleware {

    public static final String REMOVE_SESSION_COOKIE_SIGNAL = "REMOVE_SESSION_COOKIE_SIGNAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddleware.class);

    private static final String DEFAULT_SESSION_COOKIE_NAME = "inventage-portal-gateway.session";

    private final String sessionCookieName;

    public ResponseSessionCookieRemovalMiddleware(String sessionCookieName) {
        this.sessionCookieName = (sessionCookieName == null) ? DEFAULT_SESSION_COOKIE_NAME : sessionCookieName;
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
