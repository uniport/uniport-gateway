package com.inventage.portal.gateway.proxy.middleware.session;

import static io.vertx.ext.web.handler.impl.SessionHandlerImpl.SESSION_FLUSHED_KEY;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for customizing the session management.
 */
public class SessionMiddleware implements Middleware {

    public static final String SESSION_COOKIE_NAME_DEFAULT = "uniport.session";

    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";
    public static final String SESSION_LIFETIME_COOKIE_NAME_DEFAULT = "uniport.session-lifetime";
    public static final String SESSION_LIFETIME_HEADER_NAME_DEFAULT = "x-uniport-session-lifetime";

    public static final boolean COOKIE_HTTP_ONLY_DEFAULT = true;
    public static final boolean COOKIE_SECURE_DEFAULT = false;
    public static final CookieSameSite COOKIE_SAME_SITE_DEFAULT = CookieSameSite.STRICT;
    public static final int SESSION_IDLE_TIMEOUT_IN_MINUTE_DEFAULT = 15;
    public static final int SESSION_ID_MINIMUM_LENGTH_DEFAULT = 32;
    public static final boolean NAG_HTTPS_DEFAULT = true;
    public static final boolean SESSION_LIFETIME_HEADER_DEFAULT = false;
    public static final boolean SESSION_LIFETIME_COOKIE_DEFAULT = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionMiddleware.class);

    private static final int MILLIS = 60000;

    private final String name;

    private final Long sessionIdleTimeoutInMilliSeconds;

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
     */
    public SessionMiddleware(
        Vertx vertx,
        String name,
        Long sessionIdleTimeoutInMinutes,
        Boolean withLifetimeHeader,
        Boolean withLifetimeCookie,
        String cookieName,
        Boolean cookieHttpOnly,
        Boolean cookieSecure,
        String cookieSameSite,
        Integer sessionIdMinLength,
        Boolean nagHttps,
        String uriWithoutSessionIdleTimeoutReset

    ) {
        this.name = name;
        this.sessionIdleTimeoutInMilliSeconds = sessionIdleTimeoutInMinutes == null ? SESSION_IDLE_TIMEOUT_IN_MINUTE_DEFAULT * MILLIS : sessionIdleTimeoutInMinutes * MILLIS;
        this.withLifetimeHeader = withLifetimeHeader == null ? SESSION_LIFETIME_HEADER_DEFAULT : withLifetimeHeader;
        this.withLifetimeCookie = withLifetimeCookie == null ? SESSION_LIFETIME_COOKIE_DEFAULT : withLifetimeCookie;
        sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx))
            .setSessionTimeout(this.sessionIdleTimeoutInMilliSeconds)
            .setSessionCookieName(cookieName == null ? SESSION_COOKIE_NAME_DEFAULT : cookieName)
            .setCookieHttpOnlyFlag(cookieHttpOnly == null ? COOKIE_HTTP_ONLY_DEFAULT : cookieHttpOnly)
            .setCookieSecureFlag(cookieSecure == null ? COOKIE_SECURE_DEFAULT : cookieSecure)
            .setCookieSameSite(
                cookieSameSite == null ? COOKIE_SAME_SITE_DEFAULT : CookieSameSite.valueOf(cookieSameSite))
            .setMinLength(sessionIdMinLength == null ? SESSION_ID_MINIMUM_LENGTH_DEFAULT : sessionIdMinLength)
            .setNagHttps(nagHttps == null ? NAG_HTTPS_DEFAULT : nagHttps);
        this.uriPatternForIgnoringSessionTimeoutReset = uriWithoutSessionIdleTimeoutReset == null ? null : Pattern.compile(uriWithoutSessionIdleTimeoutReset);
    }

    @Override
    public void handle(RoutingContext ctx) {
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
            LOGGER.debug("Adding header '{}'", SESSION_LIFETIME_HEADER_NAME_DEFAULT);
            ctx.response().putHeader(SESSION_LIFETIME_HEADER_NAME_DEFAULT, sessionLifetime);
        }
        if (withLifetimeCookie) {
            LOGGER.debug("Adding cookie '{}'", SESSION_LIFETIME_COOKIE_NAME_DEFAULT);
            ctx.response().addCookie(
                Cookie.cookie(SESSION_LIFETIME_COOKIE_NAME_DEFAULT, sessionLifetime).setPath("/")
                    .setHttpOnly(false)); // false := cookie must be accessible by client side scripts
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
