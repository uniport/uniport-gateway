package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove the session cookie from the response, if the context holds a data for the key `REMOVE_SESSION_COOKIE_SIGNAL`.
 * The `REMOVE_SESSION_COOKIE_SIGNAL` is set in the ReplacedSessionCookieDetection whenever we detect that the user has made a request with
 * a replaced "previously" valid session (replaced by session.regenerateId()). We need to remove the session cookie or else the browser
 * will restore the replaced (now invalid) session cookie. For more details refer to
 * {@link com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddleware}
 * <p>
 * {@link #sessionCookieName} specifies the name (can be configured) of the session cookie. Keep in mind that the name has to be same as
 * the cookie name in the {@link com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware}
 */
public class ResponseSessionCookieRemovalMiddleware extends TraceMiddleware {

    private static final String REMOVE_SESSION_COOKIE_SIGNAL = "REMOVE_SESSION_COOKIE_SIGNAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddleware.class);

    private final String name;
    private final String sessionCookieName;

    public ResponseSessionCookieRemovalMiddleware(String name, String sessionCookieName) {
        this.name = name;
        this.sessionCookieName = sessionCookieName;
    }

    /**
     * @param ctx
     */
    public static void addSignal(RoutingContext ctx) {
        ctx.put(ResponseSessionCookieRemovalMiddleware.REMOVE_SESSION_COOKIE_SIGNAL, new Object());
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        // endHandler execution order: headersEndHandler -> bodyEndHandler -> endHandler
        ctx.addHeadersEndHandler(v -> removeSessionCookie(ctx));
        ctx.next();
    }

    protected void removeSessionCookie(RoutingContext ctx) {
        if (ctx.get(REMOVE_SESSION_COOKIE_SIGNAL) != null) {
            final Cookie sessionCookie = ctx.request().getCookie(sessionCookieName);
            if (sessionCookie != null) {
                LOGGER.debug("with value '{}'", sessionCookie.getValue());
            }
            // session cookie should only be removed from response, not unset in client
            final boolean invalidate = false;
            ctx.request().response().removeCookie(sessionCookieName, invalidate);
        }
    }
}
