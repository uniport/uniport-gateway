package com.inventage.portal.gateway.core.session;

import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

import static com.inventage.portal.gateway.core.entrypoint.Entrypoint.SESSION_COOKIE_NAME;
import static io.vertx.core.http.Cookie.cookie;

/**
 * Handle situations, where the browser has sent requests during the session id regeneration.
 * These situations are detected by checking the session state and the value of an additional cookie. If such
 * a situation is detected, the request is sent back as a redirect (=retry) to the same URL but without returning a
 * new session id.
 * See also Portal-Gateway.drawio for a visual explanation.
 */
public class ReplacedSessionCookieDetectionHandler implements Handler<RoutingContext> {

    public static final String COOKIE_NAME = "ipg.state";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacedSessionCookieDetectionHandler.class);
    private static final String SESSION_COOKIE_PREFIX = SESSION_COOKIE_NAME + "=";

    // wait time in ms before retry is sent to the browser
    private static final int WAIT_BEFORE_RETRY_MS = 50;

    public static Handler<RoutingContext> create() {
        return new ReplacedSessionCookieDetectionHandler();
    }

    @Override
    public void handle(RoutingContext ctx) {
        // detect invalid cookie for an already authenticated client (because of session.regenerateId())
        if (isReplacedSessionCookie(ctx)) {
            retryWithCookieFromBrowser(ctx);
            return;
        }

        ctx.addHeadersEndHandler(v -> responseWithCookie(ctx));
        ctx.next();
    }

    private boolean isReplacedSessionCookie(RoutingContext ctx) {
        return noUserInSession(ctx) && isCookieValueWithInLimit(ctx);
    }

    private void retryWithCookieFromBrowser(RoutingContext ctx) {
        ctx.response().addCookie(cookie(COOKIE_NAME, incrementCookieValue(ctx)).setPath("/").setHttpOnly(true));
        ctx.put(ResponseSessionCookieHandler.REMOVE_SESSION_COOKIE_SIGNAL, new Object());
        // delay before retry
        ctx.vertx().setTimer(WAIT_BEFORE_RETRY_MS, v -> {
            LOGGER.debug("retryWithCookieFromBrowser: invalid sessionId cookie for state, delayed retry for '{}' ms", WAIT_BEFORE_RETRY_MS);
            redirectForRetry(ctx);
        });
    }

    private void redirectForRetry(RoutingContext ctx) {
        ctx.response()
                .setStatusCode(307) // redirect by using the same HTTP method (307)
                .putHeader(HttpHeaders.LOCATION, ctx.request().uri())
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
                .end("Redirecting for retry to " + ctx.request().uri() + ".");
    }

    private boolean isCookieValueWithInLimit(RoutingContext ctx) {
        final Optional<Cookie> cookie = getCookie(ctx);
        if (cookie.isPresent()) {
            return new DetectionCookieValue(cookie.get().getValue()).isWithInLimit();
        }
        return false;
    }

    private Optional<Cookie> getCookie(RoutingContext ctx) {
        final Cookie cookie = ctx.request().getCookie(COOKIE_NAME);
        return cookie != null ? Optional.of(cookie) : Optional.empty();
    }

    private String incrementCookieValue(RoutingContext ctx) {
        final Optional<Cookie> cookie = getCookie(ctx);
        final DetectionCookieValue detectionCookieValue = new DetectionCookieValue(cookie.get().getValue());
        final String newValue = detectionCookieValue.increment();
        LOGGER.debug("incrementCookieValue: new value is '{}'", newValue);
        return String.valueOf(newValue);
    }

    private void responseWithCookie(RoutingContext ctx) {
        if (isUserInSession(ctx)) {
            LOGGER.debug("responseWithCookie: adding cookie '{}'", COOKIE_NAME);
            ctx.response().addCookie(cookie(COOKIE_NAME, new DetectionCookieValue().toString()).setPath("/").setHttpOnly(true));
        }
    }

    private boolean isUserInSession(RoutingContext ctx) {
        return ctx.user() != null;
    }

    private boolean noUserInSession(RoutingContext ctx) {
        if (isUserInSession(ctx)) {
            return false;
        }
        final io.netty.handler.codec.http.cookie.Cookie sessionCookie = getCookieFromHeader(ctx, SESSION_COOKIE_PREFIX);
        if (sessionCookie != null) {
            LOGGER.debug("noUserInSession: for received session cookie value '{}'", sessionCookie.value());
        }
        else {
            LOGGER.debug("noUserInSession: no session cookie '{}' received", SESSION_COOKIE_PREFIX);
        }
        return true;
    }

    private boolean cookieReceived(RoutingContext ctx, String cookieName) {
        if (ctx.request().getCookie(cookieName) != null) {
            return true;
        }
        else {
            return getCookieFromHeader(ctx, cookieName) != null;
        }
    }

    // we can't use ctx.request().getCookie(), so we must read the HTTP header by ourselves
    private io.netty.handler.codec.http.cookie.Cookie getCookieFromHeader(RoutingContext ctx, String cookieName) {
        return CookieUtil.cookieMapFromRequestHeader(ctx.request().headers().getAll(HttpHeaders.COOKIE))
                .get(cookieName);
    }


}
