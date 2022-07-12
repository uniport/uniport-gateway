package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages the authentication bearer. If the user is authenticated it provides either an ID token or
 * an access token as defined in the sessionScope. Access tokens are only provided if the
 * sessionScope matches the corresponding scope of the OAuth2 provider. It also ensures that no
 * token is sent to the Client.
 */
public class LanguageCookieMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddleware.class);
    public static final String IPS_LANGUAGE_COOKIE_NAME = "ips.language";

    public LanguageCookieMiddleware() {
    }

    @Override
    public void handle(RoutingContext ctx) {
        final Cookie cookie = ctx.request().getCookie(IPS_LANGUAGE_COOKIE_NAME);

        if (cookie != null) {
            LOGGER.debug("Extracted '{}' cookie with following available iso-code: '{}'", IPS_LANGUAGE_COOKIE_NAME,
                    cookie.getValue());
            ctx.request().headers().remove(HttpHeaders.ACCEPT_LANGUAGE);
            ctx.request().headers().add(HttpHeaders.ACCEPT_LANGUAGE, cookie.getValue());
        }

        ctx.next();
    }
}
