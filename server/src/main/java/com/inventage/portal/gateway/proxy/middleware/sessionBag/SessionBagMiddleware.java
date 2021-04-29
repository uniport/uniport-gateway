package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages request/response cookies. It removes all cookies from responses,
 * stores them in the session and sets them again for follow up requests.
 * It guarantees that no cookies except the session ID is sent to the client.
 */
public class SessionBagMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddleware.class);

    public static final String SESSION_BAG_COOKIES = "sessionBagCookies";

    public SessionBagMiddleware() {}

    @Override
    public void handle(RoutingContext ctx) {

        // on request: set cookie from session if present
        if (ctx.session().data().containsKey(SESSION_BAG_COOKIES)) {
            LOGGER.debug("handle: Cookies in session found. Setting as cookie header.");

            List<String> requestCookies = ctx.request().headers().getAll(HttpHeaders.COOKIE);
            ctx.request().headers().remove(HttpHeaders.COOKIE);

            Map<String, Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
            List<String> storedCookiesStr = new ArrayList<String>();
            for (Cookie storedCookie : storedCookies.values()) {
                if (cookieMatchesRequest(ctx.request().host(), ctx.request().path(),
                        storedCookie)) {
                    storedCookiesStr.add(storedCookie.encode());
                }
            }

            // https://github.com/vert-x3/vertx-web/issues/1716
            String cookieSeparator = "; "; // RFC 6265 4.2.1
            String cookies =
                    String.join(cookieSeparator, String.join(cookieSeparator, requestCookies),
                            String.join(cookieSeparator, storedCookiesStr));

            ctx.request().headers().add(HttpHeaders.COOKIE.toString(), cookies);
        }

        // on response: remove cookies if present and store them in session
        Handler<MultiMap> respHeadersModifier = headers -> {
            List<String> cookiesToAdd = headers.getAll(HttpHeaders.SET_COOKIE);
            if (cookiesToAdd == null || cookiesToAdd.isEmpty()) {
                return;
            }

            LOGGER.debug("handle: Set-Cookie detected. Removing and storing in session.");
            headers.remove(HttpHeaders.SET_COOKIE);

            Map<String, Cookie> existingCookies = ctx.session().get(SESSION_BAG_COOKIES);
            if (existingCookies == null) {
                existingCookies = new HashMap<String, Cookie>();
            }

            for (String cookieToAdd : cookiesToAdd) {
                io.netty.handler.codec.http.cookie.Cookie nettyCookie =
                        ClientCookieDecoder.STRICT.decode(cookieToAdd);
                Cookie vertxCookie = new CookieImpl(nettyCookie);
                existingCookies.put(vertxCookie.getName(), vertxCookie);
                LOGGER.debug("handler: Storing cookie '{}' in session", vertxCookie.encode());
            }
            ctx.session().put(SESSION_BAG_COOKIES, existingCookies);
        };
        this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);

        ctx.next();
    }

    // RFC 6265 https://tools.ietf.org/html/rfc6265#section-5.1.3
    private boolean cookieMatchesRequest(String host, String path, Cookie cookie) {
        boolean matches = true;

        // String domain = host.split(":")[0];
        // if (cookie.getDomain() == null || !domain.equalsIgnoreCase(cookie.getDomain())) {
        //     matches = false;
        // }

        if (cookie.getPath() == null || !path.startsWith(cookie.getPath())) {
            matches = false;
        }

        return matches;
    }
}
