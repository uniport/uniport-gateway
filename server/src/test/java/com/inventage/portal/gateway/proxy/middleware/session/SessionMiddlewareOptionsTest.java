package com.inventage.portal.gateway.proxy.middleware.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class SessionMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final Integer idleTime = 42;
        final Integer idMinLength = 32;
        final Boolean nagHttps = true;
        final String ignoreSessionTimeoutResetForURI = "aURI";
        final Boolean lifetimeCookie = true;
        final Boolean lifetimeHeader = true;
        final String cookieName = "aCookieName";
        final Boolean cookieHttpOnly = true;
        final Boolean cookieSecure = true;
        final CookieSameSite cookieSameSite = CookieSameSite.LAX;
        final Integer clusteredSessionStoreTimeout = 1234;

        final JsonObject json = JsonObject.of(
            SessionMiddlewareFactory.SESSION_IDLE_TIMEOUT_IN_MINUTES, idleTime,
            SessionMiddlewareFactory.SESSION_ID_MIN_LENGTH, idMinLength,
            SessionMiddlewareFactory.SESSION_NAG_HTTPS, nagHttps,
            SessionMiddlewareFactory.SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, ignoreSessionTimeoutResetForURI,
            SessionMiddlewareFactory.SESSION_LIFETIME_COOKIE, lifetimeCookie,
            SessionMiddlewareFactory.SESSION_LIFETIME_HEADER, lifetimeHeader,
            SessionMiddlewareFactory.SESSION_COOKIE, Map.of(
                SessionMiddlewareFactory.SESSION_COOKIE_NAME, cookieName,
                SessionMiddlewareFactory.SESSION_COOKIE_HTTP_ONLY, cookieHttpOnly,
                SessionMiddlewareFactory.SESSION_COOKIE_SECURE, cookieSecure,
                SessionMiddlewareFactory.SESSION_COOKIE_SAME_SITE, cookieSameSite),
            SessionMiddlewareFactory.CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MS, clusteredSessionStoreTimeout

        );

        // when
        final ThrowingSupplier<SessionMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), SessionMiddlewareOptions.class);

        // then
        final SessionMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(idleTime, options.getIdleTimeoutMinutes());
        assertEquals(idMinLength, options.getIdMinLength());
        assertEquals(nagHttps, options.nagHttps());
        assertEquals(ignoreSessionTimeoutResetForURI, options.getIgnoreSessionTimeoutResetForURI());
        assertEquals(lifetimeCookie, options.useLifetimeCookie());
        assertEquals(lifetimeHeader, options.useLifetimeHeader());

        assertNotNull(options.getSessionCookie());
        assertEquals(cookieName, options.getSessionCookie().getName());
        assertEquals(cookieSecure, options.getSessionCookie().isSecure());
        assertEquals(cookieHttpOnly, options.getSessionCookie().isHTTPOnly());
        assertEquals(cookieSameSite, options.getSessionCookie().getSameSite());

        assertEquals(clusteredSessionStoreTimeout, options.getClusteredSessionStoreRetryTimeoutMs());
    }
}
