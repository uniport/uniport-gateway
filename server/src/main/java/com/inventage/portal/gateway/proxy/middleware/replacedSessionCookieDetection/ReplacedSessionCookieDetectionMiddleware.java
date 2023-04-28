package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import static io.vertx.core.http.Cookie.cookie;

import com.inventage.portal.gateway.proxy.middleware.HttpResponder;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.responseSessionCookie.ResponseSessionCookieRemovalMiddleware;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieUtil;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle situations, where the browser has sent requests during the session id regeneration.
 * These situations are detected by checking the session state and the value of an additional cookie. If such
 * a situation is detected, the request is sent back as a redirect (=retry) to the same URL but without returning a
 * new session id.
 * See also Portal-Gateway.drawio for a visual explanation.
 */
public class ReplacedSessionCookieDetectionMiddleware implements Middleware {

    public static final String DEFAULT_DETECTION_COOKIE_NAME = "uniport.state";

    public static final String DEFAULT_SESSION_COOKIE_NAME = SessionMiddleware.SESSION_COOKIE_NAME_DEFAULT;
    public static final int DEFAULT_WAIT_BEFORE_RETRY_MS = 50;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacedSessionCookieDetectionMiddleware.class);
    private final String name;
    private final String detectionCookieKey;
    private final String sessionCookiePrefix;
    // wait time in ms before retry is sent to the browser
    private final int waitBeforeRetryMs;

    public ReplacedSessionCookieDetectionMiddleware(String name, String cookieName, Integer waitBeforeRetryInMs) {
        this.name = name;
        this.detectionCookieKey = (cookieName == null) ? DEFAULT_DETECTION_COOKIE_NAME : cookieName;
        this.sessionCookiePrefix = this.detectionCookieKey + "=";
        this.waitBeforeRetryMs = (waitBeforeRetryInMs == null) ? DEFAULT_WAIT_BEFORE_RETRY_MS : waitBeforeRetryInMs;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        if (requestComingFromLoggedOutUser(ctx)) {
            setDetectionCookieTo(ctx.response(), Optional.empty());
            ctx.next();
            return;
        }

        if (requestComingFromReplacedSessionId(ctx)) {
            retryWithNewSessionIdFromBrowser(ctx);
            return;
        }

        ctx.addHeadersEndHandler(v -> responseWithDetectionCookie(ctx));
        ctx.next();
    }

    private void setDetectionCookieTo(HttpServerResponse response, Optional<DetectionCookieValue> cookieValue) {
        LOGGER.debug("'{}'", this.detectionCookieKey);
        response.addCookie(
            cookie(this.detectionCookieKey, cookieValue.isPresent() ? cookieValue.get().toString() : "")
                .setPath("/").setHttpOnly(true));
    }

    /**
     * @return true, if an <strong>authenticated</strong> user's request is sent with the replaced session (but now invalid, due do session.regenerateId(),
     *         which has generated a new id for this session) <strong>session id</strong>. This special case is detected by checking (1) if the request contains
     *         our detection cookie (+ satisfies some conditions) ({@link #isDetectionCookieValueWithInLimit(RoutingContext)} and (2) if the user is not
     *         authenticated from the portal gateway's point of view (because the <strong>session id</strong> has changed)
     *         ({@link #noUserInSession(RoutingContext)}).
     *         <p>
     *         Detection-cookies are only handed to <strong>authenticated</strong> users on each new request (with
     *         {@link #responseWithDetectionCookie(RoutingContext)}).
     *         Therefore the detection-cookie serves as proof that the user has been authenticated before.
     */
    private boolean requestComingFromReplacedSessionId(RoutingContext ctx) {
        return noUserInSession(ctx) && isDetectionCookieValueWithInLimit(ctx);
    }

    /**
     * If the session cookie (uniport.session) is not received AND the state cookie (uniport.state)
     * is received, then we assume a user as previously logged out.
     *
     * @param ctx
     *            the current routing context
     * @return true if the user is assumed as logged out
     */
    private boolean requestComingFromLoggedOutUser(RoutingContext ctx) {
        return this.noSessionCookie(ctx) && getDetectionCookie(ctx).isPresent();
    }

    /**
     * Sends a redirect (retry) response after a configurable delay. We expect/hope that the browser has received the new session id in the meantime.
     * (Refer to Portal-Gateway.drawio for a visual explanation).
     * We also use the detection cookie to track how many requests were made with the previously valid <strong>session id</strong>.
     */
    private void retryWithNewSessionIdFromBrowser(RoutingContext ctx) {
        ctx.response().addCookie(
            cookie(this.detectionCookieKey, incrementDetectionCookieValue(ctx)).setPath("/").setHttpOnly(true));
        ResponseSessionCookieRemovalMiddleware.addSignal(ctx);
        // delay before retry
        ctx.vertx().setTimer(this.waitBeforeRetryMs, v -> {
            LOGGER.info("Invalid sessionId cookie for state, delayed retry for '{}' ms", this.waitBeforeRetryMs);
            HttpResponder.respondWithRedirectForRetry(ctx);
        });
    }

    /**
     * @return true if our cookie-entry exists and its value is still valid.
     *         Only authenticated users receive this cookie entry (see {@link #responseWithDetectionCookie(RoutingContext)})
     */
    private boolean isDetectionCookieValueWithInLimit(RoutingContext ctx) {
        final Optional<Cookie> cookie = getDetectionCookie(ctx);
        if (cookie.isPresent()) {
            return new DetectionCookieValue(cookie.get().getValue()).isWithInLimit();
        }
        return false;
    }

    private Optional<Cookie> getDetectionCookie(RoutingContext ctx) {
        final Cookie cookie = ctx.request().getCookie(this.detectionCookieKey);
        return cookie != null ? Optional.of(cookie) : Optional.empty();
    }

    private String incrementDetectionCookieValue(RoutingContext ctx) {
        final Optional<Cookie> cookie = getDetectionCookie(ctx);
        final DetectionCookieValue detectionCookieValue = new DetectionCookieValue(cookie.get().getValue());
        final String newValue = detectionCookieValue.increment();
        LOGGER.debug("New value is '{}'", newValue);
        return String.valueOf(newValue);
    }

    private void responseWithDetectionCookie(RoutingContext ctx) {
        if (isUserInSession(ctx)) {
            setDetectionCookieTo(ctx.response(), Optional.of(new DetectionCookieValue()));
        }
    }

    private boolean isUserInSession(RoutingContext ctx) {
        return ctx.user() != null;
    }

    private boolean noSessionCookie(RoutingContext ctx) {
        final Cookie cookie = ctx.request().getCookie(ReplacedSessionCookieDetectionMiddleware.DEFAULT_SESSION_COOKIE_NAME);
        return cookie == null;
    }

    /**
     * @param ctx
     *            the current routing context
     * @return true if no user is associated with this session: Either (a) the user has not been authenticated or (b) the session id has expired (due do
     *         session.regenerateId()).
     */
    private boolean noUserInSession(RoutingContext ctx) {
        if (isUserInSession(ctx)) {
            return false;
        }
        final io.netty.handler.codec.http.cookie.Cookie sessionCookie = getCookieFromHeader(ctx,
            this.sessionCookiePrefix);
        if (sessionCookie != null) {
            LOGGER.debug("For received session cookie value '{}'", sessionCookie.value());
        } else {
            LOGGER.debug("No session cookie '{}' received", this.sessionCookiePrefix);
        }
        return true;
    }

    // we can't use ctx.request().getCookie(), so we must read the HTTP header by ourselves
    private io.netty.handler.codec.http.cookie.Cookie getCookieFromHeader(RoutingContext ctx, String cookieName) {
        return CookieUtil.cookieMapFromRequestHeader(ctx.request().headers().getAll(HttpHeaders.COOKIE))
            .get(cookieName);
    }

}
