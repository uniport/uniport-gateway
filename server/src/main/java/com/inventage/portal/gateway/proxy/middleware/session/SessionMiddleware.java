package com.inventage.portal.gateway.proxy.middleware.session;

import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER_NAME;
import static io.vertx.ext.web.handler.impl.SessionHandlerImpl.SESSION_FLUSHED_KEY;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for customizing the session management.
 */
public class SessionMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddleware.class);

    private static final int MINUTE_MILIS = 60_000;

    private final String name;

    private final long sessionIdleTimeoutInMilliSeconds;

    private final boolean withLifetimeHeader;
    private final boolean withLifetimeCookie;

    private final Handler<RoutingContext> sessionHandler;

    private final Pattern uriPatternForIgnoringSessionTimeoutReset;

    /**
     * @param vertx
     *            current instance of Vert.x
     * @param name
     *            name for this SessionMiddleware instance
     * @param sessionIdleTimeoutInMinutes
     *            default is 15 minutes
     * @param withLifetimeHeader
     *            switch if session lifetime header should be set
     * @param withLifetimeCookie
     *            switch if session lifetime cookie should be set
     * @param cookieName
     *            name of the session cookie
     * @param cookieHttpOnly
     *            switch if the session cookie can only be accessed by the browser (and not via JS)
     * @param cookieSecure
     *            switch if the session cookie is marked as secure
     * @param cookieSameSite
     *            same site settings for the session cookie
     * @param sessionIdMinLength
     *            length of the session id
     * @param nagHttps
     *            switch if a nagging log should be written when access is not via HTTPS
     * @param uriWithoutSessionIdleTimeoutReset
     *            null or regex for specifying uri with no session timeout reset
     * @param clusteredSessionStoreRetryTimeoutMiliSeconds
     *            default retry time out, in ms, for a session not found in the clustered store.
     */
    public SessionMiddleware(
        Vertx vertx,
        String name,
        int sessionIdleTimeoutInMinutes,
        boolean withLifetimeHeader,
        boolean withLifetimeCookie,
        String cookieName,
        boolean cookieHttpOnly,
        boolean cookieSecure,
        CookieSameSite cookieSameSite,
        int sessionIdMinLength,
        boolean nagHttps,
        String uriWithoutSessionIdleTimeoutReset,
        int clusteredSessionStoreRetryTimeoutMiliSeconds
    ) {
        this.name = name;
        this.sessionIdleTimeoutInMilliSeconds = sessionIdleTimeoutInMinutes * MINUTE_MILIS;
        this.withLifetimeHeader = withLifetimeHeader;
        this.withLifetimeCookie = withLifetimeCookie;

        final SessionStore sessionStore;
        if (vertx.isClustered()) {
            LOGGER.info("Running clustered session store");
            sessionStore = ClusteredSessionStore.create(vertx, clusteredSessionStoreRetryTimeoutMiliSeconds);
        } else {
            LOGGER.info("Running local session store");
            sessionStore = LocalSessionStore.create(vertx);
        }

        sessionHandler = SessionHandler.create(sessionStore)
            .setSessionTimeout(this.sessionIdleTimeoutInMilliSeconds)
            .setSessionCookieName(cookieName)
            .setCookieHttpOnlyFlag(cookieHttpOnly)
            .setCookieSecureFlag(cookieSecure)
            .setCookieSameSite(cookieSameSite)
            .setMinLength(sessionIdMinLength)
            .setNagHttps(nagHttps);
        this.uriPatternForIgnoringSessionTimeoutReset = uriWithoutSessionIdleTimeoutReset == null ? null : Pattern.compile(uriWithoutSessionIdleTimeoutReset);
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        registerHandlerForRespondingWithSessionLifetime(ctx);
        checkForSessionTimeoutReset(ctx);

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
            LOGGER.debug("Adding header '{}'", DEFAULT_SESSION_LIFETIME_HEADER_NAME);
            ctx.response().putHeader(DEFAULT_SESSION_LIFETIME_HEADER_NAME, sessionLifetime);
        }
        if (withLifetimeCookie) {
            LOGGER.debug("Adding cookie '{}'", DEFAULT_SESSION_LIFETIME_COOKIE_NAME);
            ctx.response().addCookie(
                Cookie.cookie(DEFAULT_SESSION_LIFETIME_COOKIE_NAME, sessionLifetime).setPath("/")
                    .setHttpOnly(false) // false := cookie must be accessible by client side scripts
                    .setSameSite(CookieSameSite.STRICT)); // prevent warnings in Firefox console
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
            LOGGER.debug("Ignored for uri '{}'", requestUri);
        }
    }

    protected boolean isRequestUriMatching(String requestUri) {
        return uriPatternForIgnoringSessionTimeoutReset.matcher(requestUri).matches();
    }
}
