package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddleware;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class SessionAdapter {

    private static final int SESSION_ID_DISPLAYED_LENGTH = 5;

    public static final String EMPTY_SESSION_DISPLAY = null;

    public static String displaySessionId(RoutingContext context) {
        final Cookie cookie = context.request()
            .getCookie(ReplacedSessionCookieDetectionMiddleware.DEFAULT_SESSION_COOKIE_NAME);
        return cookie == null ? EMPTY_SESSION_DISPLAY : firstCharactersOf(SESSION_ID_DISPLAYED_LENGTH, cookie.getValue());
    }

    public static String displaySessionId(Session session) {
        return session == null ? EMPTY_SESSION_DISPLAY : firstCharactersOf(SESSION_ID_DISPLAYED_LENGTH, session.id());
    }

    protected static String firstCharactersOf(int displayLength, String string) {
        if (string != null && string.length() > displayLength) {
            return string.substring(0, displayLength);
        }
        return string;
    }

}
