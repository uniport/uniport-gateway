package com.inventage.portal.gateway.proxy.middleware.log;

import io.vertx.ext.web.Session;

public class SessionAdapter {

    private Session session;

    public SessionAdapter(Session session) {
        this.session = session;
    }

    public static String displaySessionId(Session session) {
        if (session == null) {
            return "";
        }
        return firstCharactersOf(5, session.id());
    }

    protected static String firstCharactersOf(int displayLength, String string) {
        if (string != null && string.length() > displayLength) {
            return string.substring(0, displayLength);
        }
        return string;
    }

}
