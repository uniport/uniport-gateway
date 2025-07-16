package ch.uniport.gateway.proxy.middleware.languageCookie;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets the 'accept-language' based on the language cookie value
 */
public class LanguageCookieMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddleware.class);

    private final String name;

    private final String languageCookieName;

    public LanguageCookieMiddleware(String name, String languageCookieName) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(languageCookieName, "languageCookieName must not be null");

        this.name = name;
        this.languageCookieName = languageCookieName;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        final Cookie cookie = ctx.request().getCookie(languageCookieName);
        if (cookie != null) {
            LOGGER.debug("Extracted '{}' cookie with following available iso-code: '{}'", languageCookieName,
                cookie.getValue());
            ctx.request().headers().set(HttpHeaders.ACCEPT_LANGUAGE, cookie.getValue());
        }

        ctx.next();
    }
}
