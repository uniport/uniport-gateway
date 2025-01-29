package com.inventage.portal.gateway.proxy.middleware.session;

import static io.vertx.ext.web.handler.impl.SessionHandlerImpl.SESSION_FLUSHED_KEY;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieUtil;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for customizing the session management.
 */
public class SessionMiddleware extends TraceMiddleware {

    public static final String SESSION_MIDDLEWARE_IDLE_TIMEOUT_IN_MS_KEY = "SESSION_MIDDLEWARE_IDLE_TIMEOUT_IN_MS";
    public static final String SESSION_MIDDLEWARE_SESSION_STORE_KEY = "SESSION_MIDDLEWARE_SESSION_STORE";

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddleware.class);

    private static final int MINUTE_MS = 60_000;
    private static final String COOKIE_DELIMITER = "; "; // RFC 6265 4.2.1

    private final String name;

    private final String sessionCookieName;
    private final long sessionIdleTimeoutInMilliSeconds;

    private final boolean withLifetimeHeader;
    private final String lifetimeHeaderName;

    private final boolean withLifetimeCookie;
    private final String lifetimeCookieName;
    private final String lifetimeCookiePath;
    private final boolean lifetimeCookieHttpOnly;
    private final boolean lifetimeCookieSecure;
    private final CookieSameSite lifetimeCookieSameSite;

    private final Handler<RoutingContext> sessionHandler;
    private final SessionStore sessionStore;

    private final Pattern uriPatternForIgnoringSessionTimeoutReset;

    /**
     * @param vertx
     *            current instance of Vert.x
     * @param name
     *            name for this SessionMiddleware instance
     * @param sessionIdMinLength
     *            length of the session id
     * @param sessionIdleTimeoutInMinutes
     *            default is 15 minutes
     * @param uriWithoutSessionIdleTimeoutReset
     *            null or regex for specifying uri with no session timeout reset
     * @param nagHttps
     *            switch if a nagging log should be written when access is not via HTTPS
     * @param sessionCookieName
     *            name of the session cookie
     * @param sessionCookieHttpOnly
     *            switch if the session cookie can only be accessed by the browser (and not via JS)
     * @param sessionCookieSecure
     *            switch if the session cookie is marked as secure
     * @param sessionCookieSameSite
     *            same site settings for the session cookie
     * @param withLifetimeHeader
     *            switch if session lifetime header should be set
     * @param lifetimeHeaderName
     *            name of the session life time header
     * @param withLifetimeCookie
     *            switch if session lifetime cookie should be set
     * @param lifetimeCookieName
     *            name of the session life time cookie
     * @param lifetimeCookiePath
     *            path of the session life time cookie
     * @param lifetimeCookieHttpOnly
     *            switch if the session lifetime cookie can only be accessed by the browser (and not via JS)
     * @param lifetimeCookieSecure
     *            switch if the session lifetime cookie is marked as secure
     * @param lifetimeCookieSameSite
     *            same site settings for the session lifetime cookie
     * @param clusteredSessionStoreRetryTimeoutMiliSeconds
     *            default retry time out, in ms, for a session not found in the clustered store.
     */
    public SessionMiddleware(
        Vertx vertx,
        String name,
        // session
        int sessionIdMinLength,
        int sessionIdleTimeoutInMinutes,
        String uriWithoutSessionIdleTimeoutReset,
        boolean nagHttps,
        // session cookie
        String sessionCookieName,
        boolean sessionCookieHttpOnly,
        boolean sessionCookieSecure,
        CookieSameSite sessionCookieSameSite,
        // lifetime
        boolean withLifetimeHeader,
        String lifetimeHeaderName,
        boolean withLifetimeCookie,
        String lifetimeCookieName,
        String lifetimeCookiePath,
        boolean lifetimeCookieHttpOnly,
        boolean lifetimeCookieSecure,
        CookieSameSite lifetimeCookieSameSite,
        // session store
        int clusteredSessionStoreRetryTimeoutMiliSeconds
    ) {
        this.name = name;
        this.sessionIdleTimeoutInMilliSeconds = sessionIdleTimeoutInMinutes * MINUTE_MS;
        this.uriPatternForIgnoringSessionTimeoutReset = uriWithoutSessionIdleTimeoutReset == null ? null : Pattern.compile(uriWithoutSessionIdleTimeoutReset);
        this.sessionCookieName = sessionCookieName;

        this.withLifetimeHeader = withLifetimeHeader;
        this.lifetimeHeaderName = lifetimeHeaderName;

        this.withLifetimeCookie = withLifetimeCookie;
        this.lifetimeCookieName = lifetimeCookieName;
        this.lifetimeCookiePath = lifetimeCookiePath;
        this.lifetimeCookieHttpOnly = lifetimeCookieHttpOnly;
        this.lifetimeCookieSecure = lifetimeCookieSecure;
        this.lifetimeCookieSameSite = lifetimeCookieSameSite;

        if (vertx.isClustered()) {
            LOGGER.info("Running clustered session store");
            sessionStore = ClusteredSessionStore.create(vertx, clusteredSessionStoreRetryTimeoutMiliSeconds);
        } else {
            LOGGER.info("Running local session store");
            sessionStore = LocalSessionStore.create(vertx);
        }

        sessionHandler = SessionHandler.create(sessionStore)
            .setSessionTimeout(this.sessionIdleTimeoutInMilliSeconds)
            .setSessionCookieName(this.sessionCookieName)
            .setCookieHttpOnlyFlag(sessionCookieHttpOnly)
            .setCookieSecureFlag(sessionCookieSecure)
            .setCookieSameSite(sessionCookieSameSite)
            .setMinLength(sessionIdMinLength)
            .setNagHttps(nagHttps);
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        registerHandlerForRespondingWithSessionLifetime(ctx);
        registerHandlerToRemoveSessionCookieFromRequestBeforeProxying(ctx);
        checkForSessionTimeoutReset(ctx);
        setSessionIdleTimeoutOnRoutingContext(ctx);
        setSessionStoreOnRoutingContext(ctx);

        this.sessionHandler.handle(ctx);
    }

    /**
     * If either withLifetimeHeader or withLifetimeCookie is true, then a HeadersEndHandler is added.
     *
     * @param ctx
     *            current routing context
     */
    private void registerHandlerForRespondingWithSessionLifetime(RoutingContext ctx) {
        if (withLifetimeHeader || withLifetimeCookie) {
            ctx.addHeadersEndHandler(v -> responseWithSessionLifetime(ctx));
        }
    }

    private void responseWithSessionLifetime(RoutingContext ctx) {
        final String sessionLifetime = new SessionLifetimeValue(ctx.session().lastAccessed(), this.sessionIdleTimeoutInMilliSeconds).toString();
        if (withLifetimeHeader) {
            LOGGER.debug("Adding header '{}'", this.lifetimeHeaderName);
            ctx.response().putHeader(this.lifetimeHeaderName, sessionLifetime);
        }
        if (withLifetimeCookie) {
            LOGGER.debug("Adding cookie '{}'", this.lifetimeCookieName);
            ctx.response().addCookie(
                Cookie.cookie(this.lifetimeCookieName, sessionLifetime)
                    .setPath(this.lifetimeCookiePath)
                    .setHttpOnly(this.lifetimeCookieHttpOnly)
                    .setSecure(this.lifetimeCookieSecure)
                    .setSameSite(this.lifetimeCookieSameSite));
        }
    }

    private void registerHandlerToRemoveSessionCookieFromRequestBeforeProxying(RoutingContext ctx) {
        this.addRequestHeadersModifier(ctx, headers -> removeSessionCookieFromRequestBeforeProxying(headers));
        LOGGER.debug("{}: Added removeSessionCookieFromRequestBeforeProxying as request header modifier", name);
    }

    /**
     * Request headers is a multi map i.e. the same key can appear multiple times or formulated differently, each key may have multiple values.
     * To safely filter the session cookie from the request header, this fact has to be respected.
     * 
     * To start, all "cookie" headers are read.
     * Each cookie header may contain multiple cookies separated by a semicolon, that have to be parsed.
     * Then, each cookie header is filtered for the session cookie and omitted, if found.
     * Finally, the cookie header is assembled back together.
     * 
     * It is important to note, that the overall structure of the cookie headers i.e.
     * how many cookie headers and what cookie are in what cookie header IS PRESERVED.
     * See: https://inventage-all.atlassian.net/browse/PORTAL-2349
     * 
     * @param headers
     *            of a request
     */
    private void removeSessionCookieFromRequestBeforeProxying(MultiMap headers) {
        final List<String> filteredCookies = headers
            .getAll(HttpHeaders.COOKIE)
            .stream()
            .map(cookieHeader -> ServerCookieDecoder.LAX.decode(cookieHeader).stream()
                .map(CookieUtil::fromNettyCookie)
                .filter(cookie -> !cookie.getName().equals(sessionCookieName))
                .map(cookie -> cookie.encode())
                .toList())
            .map(cookies -> String.join(COOKIE_DELIMITER, cookies))
            .toList();

        headers.remove(HttpHeaders.COOKIE);
        for (String cookieHeader : filteredCookies) {
            if (cookieHeader.length() == 0) {
                continue;
            }
            headers.add(HttpHeaders.COOKIE.toString(), cookieHeader);
        }
    }

    /**
     * Check if the session timeout reset should be skipped.
     *
     * @param ctx
     *            current routing context
     */
    private void checkForSessionTimeoutReset(RoutingContext ctx) {
        if (uriPatternForIgnoringSessionTimeoutReset == null) {
            return;
        }

        final String requestUri = ctx.request().uri();
        if (requestUri == null) {
            return;
        }
        if (isRequestUriMatching(requestUri)) {
            ctx.put(SESSION_FLUSHED_KEY, true);
            LOGGER.debug("Ignored session timeout reset for uri '{}'", requestUri);
        }
    }

    protected boolean isRequestUriMatching(String requestUri) {
        return uriPatternForIgnoringSessionTimeoutReset.matcher(requestUri).matches();
    }

    private void setSessionIdleTimeoutOnRoutingContext(RoutingContext ctx) {
        ctx.put(SESSION_MIDDLEWARE_IDLE_TIMEOUT_IN_MS_KEY, this.sessionIdleTimeoutInMilliSeconds);
    }

    /**
     * Make the session store available on the context for upcoming middleware to access sessions.
     * 
     * @param ctx
     *            current routing context
     */
    private void setSessionStoreOnRoutingContext(RoutingContext ctx) {
        ctx.put(SESSION_MIDDLEWARE_SESSION_STORE_KEY, this.sessionStore);
    }
}
