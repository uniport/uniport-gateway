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
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages request/response cookies. It removes all cookies from responses,
 * stores them in the session and sets them again for follow up requests.
 * It guarantees that no cookies except the session ID is sent to the client.
 * 
 * Divergences from RFC 6265:
 * - If the server omits the Path attribute, the middleware will use the "/" as the default value (instead of the request-uri path).
 * - The domain attribute is ignored i.e. all cookies are included in all requests
 */
public class SessionBagMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddleware.class);

    // These cookie are allowed to be passed back to the user agent.
    // This is required for keycloak login logic of its SPA (i.e. the keycloak admin UI)
    // https://github.com/keycloak/keycloak/blob/12.0.4/adapters/oidc/js/src/main/resources/login-status-iframe.html#L84
    private static final List<String> WHITHELISTED_COOKIE_NAMES =
            List.of("KEYCLOAK_SESSION", "KEYCLOAK_SESSION_LEGACY");
    private static final List<String> WHITHELISTED_COOKIE_PATHS = List.of("/auth/realms/master/");

    public static final String SESSION_BAG_COOKIES = "sessionBagCookies";

    public SessionBagMiddleware() {}

    @Override
    public void handle(RoutingContext ctx) {

        // on request: set cookie from session bag if present
        String cookies = loadCookiesFromSessionBag(ctx);
        if (cookies != null && !cookies.isEmpty()) {
            ctx.request().headers().add(HttpHeaders.COOKIE.toString(), cookies);
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
        List<String> encodedStoredCookies = new ArrayList<String>();
        for (Cookie storedCookie : storedCookies) {
            if (cookieMatchesRequest(storedCookie, ctx.request().isSSL(), ctx.request().path())) {
                encodedStoredCookies
                        .add(String.format("%s=%s", storedCookie.name(), storedCookie.value()));
            }
        }

        // https://github.com/vert-x3/vertx-web/issues/1716
        String cookieDelimiter = "; "; // RFC 6265 4.2.1
        String cookies = String.join(cookieDelimiter, encodedStoredCookies);
        // check for conflicting request and stored cookies
        // stored cookie have presedence to avoid cookie injection
        for (String requestCookie : requestCookies) {
            Cookie decodedRequestCookie = ClientCookieDecoder.STRICT.decode(requestCookie);
            if (this.containsCookie(storedCookies, decodedRequestCookie) != null) {
                continue;
            }
            cookies = String.join(cookieDelimiter, cookies, requestCookie);
        }

        return cookies;
    }

    private boolean cookieMatchesRequest(Cookie cookie, boolean isSSL, String path) {
        return matchesSSL(cookie, isSSL) && matchesPath(cookie, path);
    }

    private boolean matchesSSL(Cookie cookie, boolean isSSL) {
        return cookie.isSecure() == isSSL;
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
            requestPath =
                    uriPath.endsWith("/") ? uriPath.substring(0, uriPath.length() - 1) : uriPath;
        }

        if (cookie.path() == null) {
            return false;
        }

        String cookiePath;
        if (cookie.path().isEmpty()) {
            cookiePath = "/";
        } else {
            cookiePath = cookie.path().endsWith("/")
                    ? cookie.path().substring(0, cookie.path().length() - 1)
                    : cookie.path();
        }
        String regex = String.format("^%s(\\/.*)?$", escapeSpecialRegexChars(cookiePath));
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
            // use netty cookie until maxAge getter is impemented
            // https://github.com/eclipse-vertx/vert.x/issues/3906
            Cookie decodedCookieToSet = ClientCookieDecoder.STRICT.decode(cookieToSet);
            if (decodedCookieToSet.name().equals(Entrypoint.SESSION_COOKIE_NAME)) {
                continue;
            }
            if (WHITHELISTED_COOKIE_NAMES.contains(decodedCookieToSet.name())
                    && WHITHELISTED_COOKIE_PATHS.contains(decodedCookieToSet.path())) {
                // we delegate all logic for whitelisted cookies to the user agent 
                headers.add(HttpHeaders.SET_COOKIE, cookieToSet);
                continue;
            }
            updateSessionBag(storedCookies, decodedCookieToSet);
        }
        ctx.session().put(SESSION_BAG_COOKIES, storedCookies);
    }

    /*
    If a new cookie is received with the same cookie-name, and 
    path-value as a cookie that it has already stored,
    the existing cookie is evicted and replaced with the new cookie.
    Cookies can be deleted by sending a new cookie with an Expires
    attribute with a value in the past.
    */
    private void updateSessionBag(Set<Cookie> storedCookies, Cookie newCookie) {
        if (newCookie.name() == null) {
            LOGGER.warn("updateSessionBag: Ignoring cookie without a name");
            return;
        }
        if (newCookie.path() == null) {
            newCookie.setPath("/");
        }

        Cookie foundCookie = this.containsCookie(storedCookies, newCookie);
        if (foundCookie != null) {
            boolean expired = (foundCookie.maxAge() == 0L);
            storedCookies.remove(foundCookie);
            if (expired) {
                LOGGER.debug("updateSessionBag: Removing expired cookie '{}'", newCookie.name());
                return;
            }
        }

        if (newCookie.maxAge() <= 0L) {
            LOGGER.debug("updateSessionBag: Ignoring expired cookie");
            return;
        }
        LOGGER.debug("updateSessionBag: {} cookie '{}'",
                foundCookie != null ? "Updating" : "Adding", newCookie.name());
        storedCookies.add(newCookie);
    }

    private String escapeSpecialRegexChars(String regex) {
        return regex.replace("\\", "\\\\").replace("^", "\\^").replace("$", "\\$")
                .replace(".", "\\.").replace("|", "\\.").replace("?", "\\?").replace("*", "\\*")
                .replace("+", "\\+").replace("(", "\\(").replace(")", "\\)").replace("[", "\\[")
                .replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
    }

    private Cookie containsCookie(Set<Cookie> set, Cookie cookie) {
        for (Cookie c : set) {
            if (c.name().equals(cookie.name()) && c.path().equals(cookie.path())) {
                return c;
            }
        }
        return null;
    }
}
