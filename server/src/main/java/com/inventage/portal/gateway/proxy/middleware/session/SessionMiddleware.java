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

    public SessionMiddleware(Vertx vertx, long sessionIdleTimeoutInMinutes, String cookieName, boolean cookieHttpOnly, boolean cookieSecure,
                             String cookieSameSite, int sessionIdMinLength){
        sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx))
                .setSessionTimeout(sessionIdleTimeoutInMinutes*60000)
                .setSessionCookieName(cookieName)
                .setCookieHttpOnlyFlag(cookieHttpOnly)
                .setCookieSecureFlag(cookieSecure)
                .setCookieSameSite(CookieSameSite.valueOf(cookieSameSite))
                .setMinLength(sessionIdMinLength)
                .setNagHttps(true);
    }

    @Override
    public void handle(RoutingContext event) {
        this.sessionHandler.handle(event);
    }
}
