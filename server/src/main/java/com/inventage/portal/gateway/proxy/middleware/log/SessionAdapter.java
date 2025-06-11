package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class SessionAdapter {

    private static final int SESSION_ID_DISPLAYED_LENGTH = 5;

    public static final String EMPTY_SESSION_DISPLAY = null;

    public static String displaySessionId(RoutingContext context) {
        final Cookie sessionCookie = context.request().getCookie(AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME); // TODO technically this is wrong as the session cookie name is configurable
        return sessionCookie == null ? EMPTY_SESSION_DISPLAY : firstCharactersOf(SESSION_ID_DISPLAYED_LENGTH, sessionCookie.getValue());
    }

    public static String displaySessionId(Session session) {
        return session == null ? EMPTY_SESSION_DISPLAY : firstCharactersOf(SESSION_ID_DISPLAYED_LENGTH, session.id());
    }

    public static String displaySessionId(String sessionID) {
        return sessionID == null ? EMPTY_SESSION_DISPLAY : firstCharactersOf(SESSION_ID_DISPLAYED_LENGTH, sessionID);
    }

    protected static String firstCharactersOf(int displayLength, String string) {
        if (string != null && string.length() > displayLength) {
            return string.substring(0, displayLength);
        }
        return string;
    }

}
