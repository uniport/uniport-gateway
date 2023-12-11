package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the authentication bearer. If the user is authenticated it provides either an ID token or
 * an access token as defined in the sessionScope. Access tokens are only provided if the
 * sessionScope matches the corresponding scope of the OAuth2 provider. It also ensures that no
 * token is sent to the Client.
 */
public class LanguageCookieMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddleware.class);

    private final String name;

    private final String languageCookieName;

    public LanguageCookieMiddleware(String name, String languageCookieName) {
        this.name = name;
        this.languageCookieName = languageCookieName;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        Cookie cookie = ctx.request().getCookie(languageCookieName);

        // backward compatibility because of cookie name change (https://issue.inventage.com/browse/PORTAL-718)
        if (cookie == null) {
            cookie = ctx.request().getCookie("ips.language"); // support for old cookie name
        }

        if (cookie != null) {
            LOGGER.debug("Extracted '{}' cookie with following available iso-code: '{}'", languageCookieName,
                cookie.getValue());
            ctx.request().headers().remove(HttpHeaders.ACCEPT_LANGUAGE);
            ctx.request().headers().add(HttpHeaders.ACCEPT_LANGUAGE, cookie.getValue());
        }

        ctx.next();
    }
}
