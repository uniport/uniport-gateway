package com.inventage.portal.gateway.proxy.middleware.session;

import static io.vertx.ext.web.handler.impl.SessionHandlerImpl.SESSION_FLUSHED_KEY;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final long sessionIdleTimeoutMs;

    private final boolean withLifetimeHeader;
    private final String lifetimeHeaderName;

    private final boolean withLifetimeCookie;
    private final LifetimeCookieOptions lifetimeCookie;

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
     * @param sessionCookie
     *            options of the session cookie
     * @param withLifetimeHeader
     *            switch if session lifetime header should be set
     * @param lifetimeHeaderName
     *            name of the session life time header
     * @param withLifetimeCookie
     *            switch if session lifetime cookie should be set
     * @param lifetimeCookie
     *            options of the session life time cookie
     * @param clusteredSessionStoreRetryTimeoutMilliSeconds
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
        SessionCookieOptions sessionCookie,
        // lifetime
        boolean withLifetimeHeader,
        String lifetimeHeaderName,

        boolean withLifetimeCookie,
        LifetimeCookieOptions lifetimeCookie,
        // session store
        int clusteredSessionStoreRetryTimeoutMilliSeconds
    ) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sessionCookie.getName(), "sessionCookieName must not be null");
        Objects.requireNonNull(sessionCookie.getSameSite(), "sessionCookieSameSite must not be null");
        Objects.requireNonNull(lifetimeHeaderName, "lifetimeHeaderName must not be null");
        Objects.requireNonNull(lifetimeCookie.getName(), "lifetimeCookieName must not be null");
        Objects.requireNonNull(lifetimeCookie.getPath(), "lifetimeCookiePath must not be null");
        Objects.requireNonNull(lifetimeCookie.getSameSite(), "lifetimeCookieSameSite must not be null");
        // uriWithoutSessionIdleTimeoutReset is allowed to be null

        this.name = name;
        this.sessionIdleTimeoutMs = sessionIdleTimeoutInMinutes * MINUTE_MS;
        this.uriPatternForIgnoringSessionTimeoutReset = uriWithoutSessionIdleTimeoutReset == null ? null : Pattern.compile(uriWithoutSessionIdleTimeoutReset);
        this.sessionCookieName = sessionCookie.getName();

        this.withLifetimeHeader = withLifetimeHeader;
        this.lifetimeHeaderName = lifetimeHeaderName;

        this.withLifetimeCookie = withLifetimeCookie;
        this.lifetimeCookie = lifetimeCookie;

        if (vertx.isClustered()) {
            LOGGER.info("Running clustered session store");
            sessionStore = ClusteredSessionStore.create(vertx, clusteredSessionStoreRetryTimeoutMilliSeconds);
        } else {
            LOGGER.info("Running local session store");
            sessionStore = LocalSessionStore.create(vertx);
        }

        sessionHandler = SessionHandler.create(sessionStore)
            .setSessionTimeout(this.sessionIdleTimeoutMs)
            .setSessionCookieName(this.sessionCookieName)
            .setCookieHttpOnlyFlag(sessionCookie.isHTTPOnly())
            .setCookieSecureFlag(sessionCookie.isSecure())
            .setCookieSameSite(sessionCookie.getSameSite())
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
        final String sessionLifetime = new SessionLifetimeValue(ctx.session().lastAccessed(), this.sessionIdleTimeoutMs).toString();
        if (withLifetimeHeader) {
            LOGGER.debug("Adding header '{}'", this.lifetimeHeaderName);
            ctx.response().putHeader(this.lifetimeHeaderName, sessionLifetime);
        }
        if (withLifetimeCookie) {
            LOGGER.debug("Adding cookie '{}'", this.lifetimeCookie.getName());
            ctx.response().addCookie(
                Cookie.cookie(this.lifetimeCookie.getName(), sessionLifetime)
                    .setPath(this.lifetimeCookie.getPath())
                    .setHttpOnly(this.lifetimeCookie.isHTTPOnly())
                    .setSecure(this.lifetimeCookie.isSecure())
                    .setSameSite(this.lifetimeCookie.getSameSite()));
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
        // improve this, once this issue is resolved: https://github.com/eclipse-vertx/vertx-http-proxy/issues/100
        final List<String> filteredCookieHeaders = headers
            .getAll(HttpHeaders.COOKIE)
            .stream()
            .map(cookieHeader -> decodeCookieHeader(cookieHeader).stream()
                .filter(cookie -> cookie != null)
                .filter(cookie -> !cookie.name().equals(sessionCookieName))
                .map(cookie -> encodeCookie(cookie))
                .toList())
            .map(cookies -> String.join(COOKIE_DELIMITER, cookies))
            .filter(cookieHeader -> !cookieHeader.isEmpty())
            .toList();

        headers.remove(HttpHeaders.COOKIE);
        for (String cookieHeader : filteredCookieHeaders) {
            headers.add(HttpHeaders.COOKIE, cookieHeader);
        }
    }

    private Set<io.netty.handler.codec.http.cookie.Cookie> decodeCookieHeader(String header) {
        return ServerCookieDecoder.LAX.decode(header);
    }

    private String encodeCookie(io.netty.handler.codec.http.cookie.Cookie cookie) {
        return ClientCookieEncoder.LAX.encode(cookie);
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
        ctx.put(SESSION_MIDDLEWARE_IDLE_TIMEOUT_IN_MS_KEY, this.sessionIdleTimeoutMs);
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
