package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.PlatformHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages request/response cookies. It removes all cookies from responses,
 * stores them in the session and sets them again for follow-up requests.
 * It guarantees that no cookies except the session ID is sent to the client.
 * <p>
 * Divergences from RFC 6265:
 * - If the server omits the Path attribute, the middleware will use the "/" as the default value (instead of the request-uri path).
 * - The domain attribute is ignored i.e. all cookies are included in all requests
 */
public class SessionBagMiddleware implements Middleware, PlatformHandler {

    public static final String SESSION_BAG_COOKIES = "sessionBagCookies";

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddleware.class);
    // https://github.com/vert-x3/vertx-web/issues/1716
    private static final String COOKIE_DELIMITER = "; "; // RFC 6265 4.2.1

    private final String name;
    // These cookies are allowed to be passed back to the user agent.
    // This is required for some frontend logic to work properly
    // (e.g. for keycloak login logic of its admin console)
    private final JsonArray whitelistedCookies;
    private final String sessionCookieName;

    public SessionBagMiddleware(String name, JsonArray whitelistedCookies, String sessionCookieName) {
        this.name = name;
        this.whitelistedCookies = whitelistedCookies;
        this.sessionCookieName = sessionCookieName;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        if (ctx.session() == null) {
            LOGGER.debug("No session initialized. Skipping session bag middleware");
            ctx.next();
            return;
        }

        // on request: set cookie from session bag if present
        final String cookieHeaderValue = loadCookiesFromSessionBag(ctx);
        if (cookieHeaderValue != null && !cookieHeaderValue.isEmpty()) {
            ctx.request().headers().add(HttpHeaders.COOKIE.toString(), cookieHeaderValue);
        }

        // on response: remove cookies if present and store them in session bag
        final Handler<MultiMap> respHeadersModifier = headers -> storeCookiesInSessionBag(ctx, headers);
        this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);

        ctx.next();
    }

    private String loadCookiesFromSessionBag(RoutingContext ctx) {
        if (!ctx.session().data().containsKey(SESSION_BAG_COOKIES)) {
            return null;
        }
        LOGGER.debug("Cookies in session found. Setting as cookie header.");

        // LAX, otherwise cookies like "app-platform=iOS App Store" are not returned
        final Set<Cookie> requestCookies = CookieUtil
            .fromRequestHeader(ctx.request().headers().getAll(HttpHeaders.COOKIE));
        ctx.request().headers().remove(HttpHeaders.COOKIE);

        final Set<Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
        String cookieHeaderValue = encodeMatchingCookies(storedCookies, ctx);

        // check for conflicting request and stored cookies
        // stored cookie have precedence to avoid cookie injection
        for (Cookie requestCookie : requestCookies) {
            if (this.containsCookie(storedCookies, requestCookie, ctx.request().path()) != null) {
                LOGGER.debug("Ignoring cookie '{}' from request.", requestCookie.name());
                continue;
            }
            cookieHeaderValue = String.join(COOKIE_DELIMITER, cookieHeaderValue, requestCookie.toString());
        }

        return cookieHeaderValue;
    }

    private String encodeMatchingCookies(Set<Cookie> storedCookies, RoutingContext ctx) {
        final List<String> encodedStoredCookies = new ArrayList<>();
        for (Cookie storedCookie : storedCookies) {
            if (cookieMatchesRequest(storedCookie, ctx.request().isSSL(), ctx.request().path())) {
                LOGGER.debug("Add cookie '{}' to request", storedCookie.name());
                encodedStoredCookies.add(String.format("%s=%s", storedCookie.name(), storedCookie.value()));
            }
        }
        return String.join(COOKIE_DELIMITER, encodedStoredCookies);
    }

    private boolean cookieMatchesRequest(Cookie cookie, boolean isSSL, String path) {
        if (matchesSSL(cookie, isSSL) && matchesPath(cookie, path)) {
            return true;
        }
        LOGGER.debug("Ignoring cookie '{}', match path = '{}', match ssl = '{}'", cookie.name(),
            matchesPath(cookie, path), matchesSSL(cookie, isSSL));
        return false;
    }

    private boolean matchesSSL(Cookie cookie, boolean isSSL) {
        // hardcoded to true, necessary for Keycloak login flow, otherwise special handling for Keycloak
        // cookies `AUTH_SESSION` and `AUTH_SESSION_LEGACY` is required. Keycloak seems to set the secure
        // flag on both cookies if the request is forwarded via HTTPS, even if the Portal-Gateway --> Keycloak
        // connection is HTTP.
        return true;
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
        final String requestPath;
        if (!uriPath.startsWith("/") || uriPath.split("/").length - 1 <= 1) {
            requestPath = "/";
        } else {
            requestPath = uriPath.endsWith("/") ? uriPath.substring(0, uriPath.length() - 1) : uriPath;
        }

        if (cookie.path() == null) {
            return false;
        }

        final String cookiePath;
        if (cookie.path().isEmpty()) {
            cookiePath = "/";
        } else {
            cookiePath = cookie.path().endsWith("/") ? cookie.path().substring(0, cookie.path().length() - 1)
                : cookie.path();
        }
        final String regex = String.format("^%s(\\/.*)?$", escapeSpecialRegexChars(cookiePath));
        return Pattern.compile(regex).matcher(requestPath).matches();
    }

    private void storeCookiesInSessionBag(RoutingContext ctx, MultiMap headers) {
        final List<String> cookiesToSet = headers.getAll(HttpHeaders.SET_COOKIE);
        if (cookiesToSet == null || cookiesToSet.isEmpty()) {
            return;
        }

        LOGGER.debug("Set-Cookie detected. Removing and storing in session.");
        headers.remove(HttpHeaders.SET_COOKIE);

        Set<Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
        if (storedCookies == null) {
            storedCookies = new HashSet<>();
        }

        for (String cookieToSet : cookiesToSet) {
            // use netty cookie until maxAge getter is implemented
            // https://github.com/eclipse-vertx/vert.x/issues/3906
            final Cookie decodedCookieToSet = ClientCookieDecoder.STRICT.decode(cookieToSet);
            if (decodedCookieToSet.name().equals(sessionCookieName)) {
                continue;
            }
            if (isWhitelisted(decodedCookieToSet)) {
                // we delegate all logic for whitelisted cookies to the user agent
                LOGGER.debug("'Passing cookie to user agent: {}'", cookieToSet);
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
            LOGGER.warn("Ignoring cookie without a name");
            return;
        }
        if (newCookie.path() == null) {
            newCookie.setPath("/");
        }

        final Cookie foundCookie = this.containsCookie(storedCookies, newCookie);
        if (foundCookie != null) {
            final boolean expired = (foundCookie.maxAge() == 0L);
            storedCookies.remove(foundCookie);
            if (expired) {
                LOGGER.debug("Removing expired cookie '{}' from session bag", newCookie.name());
                return;
            }
        }

        if (newCookie.maxAge() == 0L) {
            LOGGER.debug("Ignoring expired cookie '{}'", newCookie.name());
            return;
        }
        LOGGER.debug("Adding new cookie '{}'", newCookie.name());
        storedCookies.add(newCookie);
    }

    private String escapeSpecialRegexChars(String regex) {
        return regex.replace("\\", "\\\\").replace("^", "\\^").replace("$", "\\$").replace(".", "\\.")
            .replace("|", "\\.").replace("?", "\\?").replace("*", "\\*").replace("+", "\\+").replace("(", "\\(")
            .replace(")", "\\)").replace("[", "\\[").replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
    }

    private Cookie containsCookie(Set<Cookie> set, Cookie cookie) {
        for (Cookie c : set) {
            if (c.name().equals(cookie.name()) && c.path().equals(cookie.path())) {
                return c;
            }
        }
        return null;
    }

    private Cookie containsCookie(Set<Cookie> set, Cookie cookie, String path) {
        for (Cookie c : set) {
            if (c.name().equals(cookie.name()) && path.startsWith(c.path())) {
                return c;
            }
        }
        return null;
    }

    private boolean isWhitelisted(Cookie cookie) {
        for (int i = 0; i < this.whitelistedCookies.size(); i++) {
            final JsonObject whitelistedCookie = this.whitelistedCookies.getJsonObject(i);
            final String whitelistedCookieName = whitelistedCookie
                .getString(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME);
            final String whitelistedCookiePath = whitelistedCookie
                .getString(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH);
            if (whitelistedCookieName.equals(cookie.name()) && whitelistedCookiePath.equals(cookie.path())) {
                return true;
            }
        }
        return false;
    }
}
