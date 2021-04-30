package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
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

    public SessionBagMiddleware() {
    }

    @Override
    public void handle(RoutingContext ctx) {

        // on request: set cookie from session bag if present
        String cookies = loadCookiesFromSessionBag(ctx);
        if (cookies != null && !cookies.isEmpty()) {
            ctx.request().headers().add(HttpHeaders.COOKIE.toString(), cookies);
            System.out.println(ctx.request());
        }

        // on response: remove cookies if present and store them in session bag
        Handler<MultiMap> respHeadersModifier = headers -> {
            storeCookiesInSessionBag(ctx, headers);
        };
        this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);

        ctx.next();
    }

    private String loadCookiesFromSessionBag(RoutingContext ctx) {
        if (!ctx.session().data().containsKey(SESSION_BAG_COOKIES)) {
            return null;
        }
        LOGGER.debug("handle: Cookies in session found. Setting as cookie header.");

        List<String> requestCookies = ctx.request().headers().getAll(HttpHeaders.COOKIE);
        ctx.request().headers().remove(HttpHeaders.COOKIE);

        Set<Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
        List<String> storedCookiesStr = new ArrayList<String>();
        for (Cookie storedCookie : storedCookies) {
            if (cookieMatchesRequest(storedCookie, ctx.request().host(), ctx.request().path())) {
                storedCookiesStr.add(storedCookie.encode());
            }
        }

        // https://github.com/vert-x3/vertx-web/issues/1716
        String cookieSeparator = "; "; // RFC 6265 4.2.1
        // TODO
        // String cookies = String.join(cookieSeparator, String.join(cookieSeparator, requestCookies),
        //         String.join(cookieSeparator, storedCookiesStr));
        String cookies = String.join(cookieSeparator, storedCookiesStr);

        return cookies;
    }

    private boolean cookieMatchesRequest(Cookie cookie, String host, String path) {
        String domain = host.split(":")[0];
        return matchesDomain(cookie, domain) && matchesPath(cookie, path);
    }

    /*
    A string domain-matches a given domain string if at least one of the
    following conditions hold:
    - The domain string and the string are identical.
    - All of the following conditions hold:
    -  The domain string is a suffix of the string.
    -  The last character of the string that is not included in the
       domain string is a %x2E (".") character.
    -  The string is a host name (i.e., not an IP address).
    
    https://tools.ietf.org/html/rfc6265#section-5.1.3
    */
    private boolean matchesDomain(Cookie cookie, String domain) {
        // if the domain attribute is ommited, the cookie will only be returned to the origin server // TODO
        if (cookie.getDomain() == null) {
            return true;
        }
        String regex = String.format("^([a-zA-Z0-9\\.]*\\.)?%s$", cookie.getDomain()).replace(".", "\\.");
        return Pattern.compile(regex).matcher(domain).matches();
    }

    /*
    The user agent MUST use an algorithm equivalent to the following
    algorithm to compute the default-path of a cookie:
     1.  Let uri-path be the path portion of the request-uri if such a
     portion exists (and empty otherwise).  For example, if the
     request-uri contains just a path (and optional query string),
     then the uri-path is that path (without the %x3F ("?") character
     or query string), and if the request-uri contains a full
     absoluteURI, the uri-path is the path component of that URI.
     2.  If the uri-path is empty or if the first character of the uri-
     path is not a %x2F ("/") character, output %x2F ("/") and skip
     the remaining steps.
     3.  If the uri-path contains no more than one %x2F ("/") character,
     output %x2F ("/") and skip the remaining step.
     4.  Output the characters of the uri-path from the first character up
     to, but not including, the right-most %x2F ("/").
    
    A request-path path-matches a given cookie-path if at least one of
     the following conditions holds:
     -  The cookie-path and the request-path are identical.
     -  The cookie-path is a prefix of the request-path, and the last
    character of the cookie-path is %x2F ("/").
     -  The cookie-path is a prefix of the request-path, and the first
    character of the request-path that is not included in the cookie-
    path is a %x2F ("/") character.
    
    https://tools.ietf.org/html/rfc6265#section-5.1.4
    */
    private boolean matchesPath(Cookie cookie, String uriPath) {
        String requestPath;
        if (uriPath.isEmpty() || !uriPath.startsWith("/") || uriPath.split("/").length - 1 <= 1) {
            requestPath = "/";
        } else {
            requestPath = uriPath.endsWith("/") ? uriPath.substring(0, uriPath.length() - 1) : uriPath;
        }

        if (cookie.getPath() == null) {
            return false;
        }

        String cookiePath;
        if (cookie.getPath().isEmpty()) {
            cookiePath = "/";
        } else {
            cookiePath = cookie.getPath().endsWith("/") ? cookie.getPath().substring(0, cookie.getPath().length() - 1)
                    : cookie.getPath();
        }
        String regex = String.format("^%s(\\/[a-zA-Z0-9\\/]*)?$", cookiePath);
        return Pattern.compile(regex).matcher(requestPath).matches();
    }

    private void storeCookiesInSessionBag(RoutingContext ctx, MultiMap headers) {
        List<String> cookiesToSet = headers.getAll(HttpHeaders.SET_COOKIE);
        if (cookiesToSet == null || cookiesToSet.isEmpty()) {
            return;
        }

        LOGGER.debug("handle: Set-Cookie detected. Removing and storing in session.");
        headers.remove(HttpHeaders.SET_COOKIE);

        Set<Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
        if (storedCookies == null) {
            storedCookies = new HashSet<Cookie>();
        }

        for (String cookieToSet : cookiesToSet) {
            io.netty.handler.codec.http.cookie.Cookie nettyCookie = ClientCookieDecoder.STRICT.decode(cookieToSet);
            Cookie vertxCookie = new CookieImpl(nettyCookie);
            if (vertxCookie.getName().equals(Entrypoint.SESSION_COOKIE_NAME)) {
                continue;
            }
            System.out.println(vertxCookie.encode());
            updateSessionBag(storedCookies, vertxCookie);
        }
        ctx.session().put(SESSION_BAG_COOKIES, storedCookies);
    }

    /*
    If a new cookie is received with the same cookie-name,
    domain-value, and path-value as a cookie that it has already stored,
    the existing cookie is evicted and replaced with the new cookie.
    Cookies can be deleted by sending a new cookie with an Expires
    attribute with a value in the past.
    */
    private void updateSessionBag(Set<Cookie> storedCookies, Cookie newCookie) {
        Cookie foundCookie = null;
        for (Cookie storedCookie : storedCookies) {
            if (storedCookie.getName().equals(newCookie.getName())
                    // && storedCookie.getDomain().equals(newCookie.getDomain()) // TODO
                    && storedCookie.getPath().equals(newCookie.getPath())) {
                foundCookie = storedCookie;
                break;
            }
        }
        boolean expired = false; // TODO check if new cookie is expired
        if (foundCookie != null) {
            storedCookies.remove(foundCookie);
            if (expired) {
                LOGGER.debug("updateSessionBag: Removing expired cookie '{}'", newCookie.encode());
                return;
            }
        }
        LOGGER.debug("updateSessionBag: {} cookie '{}'", foundCookie != null ? "Updating" : "Adding",
                newCookie.encode());
        storedCookies.add(newCookie);
    }
}
