package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import java.util.ArrayList;
import java.util.List;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages request/response cookies. It removes all cookies from responses,
 * stores them in the session and sets them again for follow up requests.
 * It guarantees that no cookies except the session ID is sent to the client.
 */
public class SessionBagMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddleware.class);

    public static final String SESSION_BAG_COOKIES = "sessionBagCookies";

    public SessionBagMiddleware() {
    }

    @Override
    public void handle(RoutingContext ctx) {

        // on request: set cookie from session if present
        if (ctx.session().data().containsKey(SESSION_BAG_COOKIES)) {
            LOGGER.debug("handle: Cookies in session found. Setting as cookie header.");

            List<String> requestCookies = ctx.request().headers().getAll(HttpHeaders.COOKIE);
            ctx.request().headers().remove(HttpHeaders.COOKIE);

            // https://github.com/vert-x3/vertx-web/issues/1716
            List<String> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
            String cookieSeparator = "; ";
            String cookies = String.join(cookieSeparator, requestCookies) + cookieSeparator
                    + String.join(cookieSeparator, storedCookies);

            ctx.request().headers().add(HttpHeaders.COOKIE.toString(), cookies);
        }

        // on response: remove cookies if present and store them in session
        Handler<MultiMap> respModifier = headers -> {
            List<String> cookiesToAdd = headers.getAll(HttpHeaders.SET_COOKIE);
            if (cookiesToAdd == null || cookiesToAdd.isEmpty()) {
                return;
            }
            LOGGER.debug("handle: Set-Cookie detected. Removing and storing in session.");
            headers.remove(HttpHeaders.SET_COOKIE);
            List<String> existingCookies = ctx.session().get(SESSION_BAG_COOKIES);
            if (existingCookies == null) {
                existingCookies = new ArrayList<String>();
            }
            for (String cookieToAdd : cookiesToAdd) {
                if (existingCookies.contains(cookieToAdd)) {
                    continue;
                }
                existingCookies.add(cookieToAdd);
                LOGGER.debug("handler: Storing cookie '{}' in session", cookieToAdd);
            }
            ctx.session().put(SESSION_BAG_COOKIES, existingCookies);
        };
        this.addResponseHeadersModifier(ctx, respModifier);

        ctx.next();
    }
}
