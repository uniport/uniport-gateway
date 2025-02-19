package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.PlatformHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
 * - If the server omits the Path attribute, the middleware will use the "/" as
 * the default value (instead of the request-uri path).
 * - The domain attribute is ignored i.e. all cookies are included in all
 * requests
 */
public class SessionBagMiddleware extends TraceMiddleware implements PlatformHandler {

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
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(whitelistedCookies, "whitelistedCookies must not be null");
        Objects.requireNonNull(sessionCookieName, "sessionCookieName must not be null");

        this.name = name;
        this.whitelistedCookies = new JsonArray(whitelistedCookies.getList());
        this.sessionCookieName = sessionCookieName;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        if (ctx.session() == null) {
            LOGGER.debug("No session initialized. Skipping session bag middleware");
            ctx.next();
            return;
        }

        // load request cookies
        final Set<Cookie> requestCookies = CookieUtil
            .fromRequestHeader(ctx.request().headers().getAll(HttpHeaders.COOKIE));
        ctx.request().headers().remove(HttpHeaders.COOKIE);

        // on incoming request: set cookie from session bag if present
        final String cookieHeaderValue = loadCookiesFromSessionBag(ctx, requestCookies);
        if (cookieHeaderValue != null && !cookieHeaderValue.isEmpty()) {
            ctx.request().headers().add(HttpHeaders.COOKIE.toString(), cookieHeaderValue);
        }

        // on outgoing response: remove cookies if present and store them in session bag
        ctx.addHeadersEndHandler(v -> storeCookiesInSessionBag(ctx, ctx.response().headers()));
        LOGGER.debug("{}: Added storeCookiesInSessionBag as response header modifier", name);

        ctx.next();
    }

    private String loadCookiesFromSessionBag(RoutingContext ctx, Set<Cookie> requestCookies) {
        if (!ctx.session().data().containsKey(SESSION_BAG_COOKIES)) {
            return null;
        }
        LOGGER.debug("Cookies in session found. Setting as cookie header.");

        // load stored cookies from session bag
        final Set<Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
        final String cookieHeaderValue = encodeMatchingCookies(storedCookies, ctx.request().path(), ctx.request().isSSL());

        return appendEncodedConflictfreeCookies(ctx.request().path(), cookieHeaderValue, requestCookies, storedCookies);
    }

    private String encodeMatchingCookies(Set<Cookie> storedCookies, String path, boolean isSSL) {
        final List<String> encodedStoredCookies = new ArrayList<>();
        for (Cookie storedCookie : storedCookies) {
            if (cookieMatchesRequest(storedCookie, isSSL, path)) {
                LOGGER.debug("Adding cookie '{}' to request", storedCookie.getName());
                encodedStoredCookies.add(encodeCooke(storedCookie.getName(), storedCookie.getValue()));
            }
        }
        return String.join(COOKIE_DELIMITER, encodedStoredCookies);
    }

    private boolean cookieMatchesRequest(Cookie cookie, boolean isSSL, String path) {
        if (matchesSSL(cookie, isSSL) && matchesPath(cookie, path)) {
            return true;
        }
        LOGGER.debug("Ignoring cookie '{}', match path = '{}', match ssl = '{}'", cookie.getName(),
            matchesPath(cookie, path), matchesSSL(cookie, isSSL));
        return false;
    }

    private boolean matchesSSL(Cookie cookie, boolean isSSL) {
        // hardcoded to true, necessary for Keycloak login flow, otherwise special
        // handling for Keycloak
        // cookies `AUTH_SESSION` and `AUTH_SESSION_LEGACY` is required. Keycloak seems
        // to set the secure
        // flag on both cookies if the request is forwarded via HTTPS, even if the
        // Portal-Gateway --> Keycloak
        // connection is HTTP.
        return true;
    }

    /*
     * The user agent MUST use an algorithm equivalent to the following
     * algorithm to compute the default-path of a cookie:
     * 1. Let uri-path be the path portion of the request-uri if such a
     * portion exists (and empty otherwise). For example, if the
     * request-uri contains just a path (and optional query string),
     * then the uri-path is that path (without the %x3F ("?") character
     * or query string), and if the request-uri contains a full
     * absoluteURI, the uri-path is the path component of that URI.
     * 2. If the uri-path is empty or if the first character of the uri-
     * path is not a %x2F ("/") character, output %x2F ("/") and skip
     * the remaining steps.
     * 3. If the uri-path contains no more than one %x2F ("/") character,
     * output %x2F ("/") and skip the remaining step.
     * 4. Output the characters of the uri-path from the first character up
     * to, but not including, the right-most %x2F ("/").
     * 
     * A request-path path-matches a given cookie-path if at least one of
     * the following conditions holds:
     * - The cookie-path and the request-path are identical.
     * - The cookie-path is a prefix of the request-path, and the last
     * character of the cookie-path is %x2F ("/").
     * - The cookie-path is a prefix of the request-path, and the first
     * character of the request-path that is not included in the cookie-
     * path is a %x2F ("/") character.
     * 
     * https://tools.ietf.org/html/rfc6265#section-5.1.4
     */
    private boolean matchesPath(Cookie cookie, String uriPath) {
        final String requestPath;
        if (!uriPath.startsWith("/") || uriPath.split("/").length - 1 <= 1) {
            requestPath = "/";
        } else {
            requestPath = uriPath.endsWith("/") ? uriPath.substring(0, uriPath.length() - 1) : uriPath;
        }

        if (cookie.getPath() == null) {
            return false;
        }

        final String cookiePath;
        if (cookie.getPath().isEmpty()) {
            cookiePath = "/";
        } else {
            cookiePath = cookie.getPath().endsWith("/") ? cookie.getPath().substring(0, cookie.getPath().length() - 1)
                : cookie.getPath();
        }
        final String regex = String.format("^%s(\\/.*)?$", escapeSpecialRegexChars(cookiePath));
        return Pattern.compile(regex).matcher(requestPath).matches();
    }

    /**
     * check for conflicting request and stored cookies
     * stored cookie have precedence to avoid cookie injection
     */
    private String appendEncodedConflictfreeCookies(String path, String cookieHeaderValue, Set<Cookie> requestCookies, Set<Cookie> storedCookies) {
        for (Cookie requestCookie : requestCookies) {
            if (this.containsCookie(storedCookies, requestCookie, path) != null) {
                LOGGER.debug("Ignoring cookie '{}' from request.", requestCookie.getName());
                continue;
            }
            cookieHeaderValue = String.join(COOKIE_DELIMITER, cookieHeaderValue, encodeCooke(requestCookie.getName(), requestCookie.getValue()));
        }
        return cookieHeaderValue;
    }

    private String encodeCooke(String name, String value) {
        return String.format("%s=%s", name, value);
    }

    private void storeCookiesInSessionBag(RoutingContext ctx, MultiMap headers) {
        final List<String> cookiesToSet = headers.getAll(HttpHeaders.SET_COOKIE);
        if (cookiesToSet == null || cookiesToSet.isEmpty()) {
            LOGGER.debug("No cookies received in header.");
            return;
        }

        LOGGER.debug("Set-Cookie detected. Removing and storing in session with  id '{}'.", ctx.session().id());
        headers.remove(HttpHeaders.SET_COOKIE);

        Set<Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
        if (storedCookies == null) {
            storedCookies = new CookieBag();
        }

        for (String cookieToSet : cookiesToSet) {
            final Cookie decodedCookieToSet = CookieUtil.fromNettyCookie(ClientCookieDecoder.STRICT.decode(cookieToSet));
            if (isSessionCookie(decodedCookieToSet)) {
                // special-case: session cookie should always be passed to the user-agent
                continue;
            }
            if (isWhitelisted(decodedCookieToSet)) {
                // whitelisted cookie are passed to the user-agent
                LOGGER.debug("Passing cookie to user agent: '{}'", cookieToSet);
                headers.add(HttpHeaders.SET_COOKIE, cookieToSet);
                continue;
            } else {
                LOGGER.debug("Cookie removed from response: '{}'", cookieToSet);
            }
            updateSessionBag(storedCookies, decodedCookieToSet);
        }
        ctx.session().put(SESSION_BAG_COOKIES, storedCookies);
    }

    /*
     * If a new cookie is received with the same cookie-name, and
     * path-value as a cookie that it has already stored,
     * the existing cookie is evicted and replaced with the new cookie.
     * Cookies can be deleted by sending a new cookie with an Expires
     * attribute with a value in the past.
     */
    private void updateSessionBag(Set<Cookie> storedCookies, Cookie newCookie) {
        if (newCookie.getName() == null) {
            LOGGER.warn("Ignoring cookie without a name");
            return;
        }
        if (newCookie.getPath() == null) {
            newCookie.setPath("/");
        }

        final Cookie foundCookie = this.containsCookie(storedCookies, newCookie);
        if (foundCookie != null) {
            final boolean expired = (foundCookie.getMaxAge() == 0L);
            storedCookies.remove(foundCookie);
            if (expired) {
                LOGGER.debug("Removing expired cookie '{}' from session bag", newCookie.getName());
                return;
            }
        }

        if (newCookie.getMaxAge() == 0L) {
            LOGGER.debug("Ignoring expired cookie '{}'", newCookie.getName());
            return;
        }
        LOGGER.debug("Adding new cookie '{}'", newCookie.getName());
        storedCookies.add(newCookie);
    }

    private String escapeSpecialRegexChars(String regex) {
        return regex.replace("\\", "\\\\").replace("^", "\\^").replace("$", "\\$").replace(".", "\\.")
            .replace("|", "\\.").replace("?", "\\?").replace("*", "\\*").replace("+", "\\+").replace("(", "\\(")
            .replace(")", "\\)").replace("[", "\\[").replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
    }

    private Cookie containsCookie(Set<Cookie> set, Cookie cookie) {
        for (Cookie c : set) {
            if (c.getName().equals(cookie.getName()) && c.getPath().equals(cookie.getPath())) {
                return c;
            }
        }
        return null;
    }

    private Cookie containsCookie(Set<Cookie> set, Cookie cookie, String path) {
        for (Cookie c : set) {
            if (c.getName().equals(cookie.getName()) && path.startsWith(c.getPath())) {
                return c;
            }
        }
        return null;
    }

    private boolean isWhitelisted(Cookie cookie) {
        for (int i = 0; i < this.whitelistedCookies.size(); i++) {
            final JsonObject whitelistedCookie = this.whitelistedCookies.getJsonObject(i);
            final String whitelistedCookieName = whitelistedCookie.getString(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_NAME);
            final String whitelistedCookiePath = whitelistedCookie.getString(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_PATH);
            if (whitelistedCookieName.equals(cookie.getName()) && whitelistedCookiePath.equals(cookie.getPath())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSessionCookie(Cookie cookie) {
        return cookie.getName().equals(sessionCookieName);
    }
}
