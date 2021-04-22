package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import java.util.List;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages request/response cookies. It removes all cookies from responses,
 * stores them in the session and sets them again for follow up requests.
 * It guarantees that no cookies except the session ID is sent to the client.
 */
public class SessionBagMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddleware.class);

    private static final String SESSION_BAG_COOKIES = "sessionBagCookies";

    public SessionBagMiddleware() {}

    @Override
    public void handle(RoutingContext ctx) {

        // on response: remove cookies if present and store them in session
        if (ctx.response().headers().contains(HttpHeaders.SET_COOKIE)) {
            LOGGER.debug("handle: Set-Cookie detected. Removing and storing in session.");
            List<String> responseCookies = ctx.response().headers().getAll(HttpHeaders.SET_COOKIE);
            ctx.response().headers().remove(HttpHeaders.SET_COOKIE);
            ctx.session().put(SESSION_BAG_COOKIES, responseCookies);
        }

        // on request: set cookie from session if present
        if (ctx.session().data().containsKey(SESSION_BAG_COOKIES)) {
            LOGGER.debug("handle: Cookies in session found. Setting as cookie header.");
            List<String> requestCookies = ctx.session().get(SESSION_BAG_COOKIES);
            ctx.request().headers().add(HttpHeaders.COOKIE.toString(), requestCookies);
        }

        ctx.next();
    }
}
