package com.inventage.portal.gateway.proxy.middleware.session;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class SessionMiddleware implements Middleware {

    private final Handler sessionHandler;

    public static final String COOKIE_NAME_DEFAULT = "inventage-portal-gateway.session";
    public static final boolean COOKIE_HTTP_ONLY_DEFAULT = true;
    public static final boolean COOKIE_SECURE_DEFAULT = false;
    public static final CookieSameSite COOKIE_SAME_SITE_DEFAULT = CookieSameSite.STRICT;
    public static final int SESSION_IDLE_TIMEOUT_IN_MINUTE_DEFAULT = 15;
    public static final int SESSION_ID_MINIMUM_LENGTH_DEFAULT = 32;
    public static boolean NAG_HTTPS_DEFAULT = true;


    public SessionMiddleware(Vertx vertx, Long sessionIdleTimeoutInMinutes, String cookieName, Boolean cookieHttpOnly, Boolean cookieSecure,
                             String cookieSameSite, Integer sessionIdMinLength, Boolean nagHttps){
        sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx))
                .setSessionTimeout(sessionIdleTimeoutInMinutes == null ? SESSION_IDLE_TIMEOUT_IN_MINUTE_DEFAULT*60000 : sessionIdleTimeoutInMinutes*60000)
                .setSessionCookieName(cookieName == null ? COOKIE_NAME_DEFAULT : cookieName)
                .setCookieHttpOnlyFlag(cookieHttpOnly == null ? COOKIE_HTTP_ONLY_DEFAULT : cookieHttpOnly)
                .setCookieSecureFlag(cookieSecure == null ? COOKIE_SECURE_DEFAULT : cookieSecure)
                .setCookieSameSite(cookieSameSite == null ? COOKIE_SAME_SITE_DEFAULT : CookieSameSite.valueOf(cookieSameSite))
                .setMinLength(sessionIdMinLength == null ? SESSION_ID_MINIMUM_LENGTH_DEFAULT : sessionIdMinLength)
                .setNagHttps(nagHttps == null ? NAG_HTTPS_DEFAULT : nagHttps);
    }

    @Override
    public void handle(RoutingContext event) {
        this.sessionHandler.handle(event);
    }
}
