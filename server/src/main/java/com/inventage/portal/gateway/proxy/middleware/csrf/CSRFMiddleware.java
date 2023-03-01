package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSRFHandler;

public class CSRFMiddleware implements Middleware {
    public static final String DEFAULT_COOKIE_NAME = CSRFHandler.DEFAULT_COOKIE_NAME;
    public static final String DEFAULT_HEADER_NAME = CSRFHandler.DEFAULT_HEADER_NAME;
    public static final String DEFAULT_COOKIE_PATH = CSRFHandler.DEFAULT_COOKIE_PATH;
    private static final int MILLISECONDS_IN_1_MINUTE = 60000;
    public static final int DEFAULT_TIMEOUT_IN_MINUTES = 15;
    public static final boolean DEFAULT_COOKIE_SECURE = true;
    public static final boolean DEFAULT_NAG_HTTPS = true;
    private final CSRFHandler csrfHandler;

    public CSRFMiddleware(Vertx vertx, String secret, String cookieName, String cookiePath, Boolean cookieSecure,
                          String headerName, Long timeoutInMinute, String origin, Boolean nagHttps) {
        this.csrfHandler = CSRFHandler.create(vertx, secret)
                .setCookieName(cookieName == null ? DEFAULT_COOKIE_NAME : cookieName)
                .setCookiePath(cookiePath == null ? DEFAULT_COOKIE_PATH : cookiePath)
                .setCookieSecure(cookieSecure == null ? DEFAULT_COOKIE_SECURE : cookieSecure)
                .setHeaderName(headerName == null ? DEFAULT_HEADER_NAME : headerName)
                .setTimeout(timeoutInMinute == null ? DEFAULT_TIMEOUT_IN_MINUTES * MILLISECONDS_IN_1_MINUTE
                        : timeoutInMinute * MILLISECONDS_IN_1_MINUTE)
                .setNagHttps(nagHttps == null ? DEFAULT_NAG_HTTPS : nagHttps);
        if(origin != null){
            this.csrfHandler.setOrigin(origin);
        }
    }

    @Override
    public void handle(RoutingContext routingContext) {
        csrfHandler.handle(routingContext);
    }
}
