package com.inventage.portal.gateway.proxy.middleware.csrf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class CSRFMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String cookieName = "aCookieName";
        final String cookiePath = "aCookiePath";
        final Boolean cookieSecure = true;
        final String headerName = "aHeader";
        final Boolean nagHttps = true;
        final String origin = "aOrigin";
        final Integer timeoutMins = 42;

        final JsonObject json = JsonObject.of(
            CSRFMiddlewareFactory.CSRF_COOKIE, Map.of(
                CSRFMiddlewareFactory.CSRF_COOKIE_NAME, cookieName,
                CSRFMiddlewareFactory.CSRF_COOKIE_PATH, cookiePath,
                CSRFMiddlewareFactory.CSRF_COOKIE_SECURE, cookieSecure),
            CSRFMiddlewareFactory.CSRF_HEADER_NAME, headerName,
            CSRFMiddlewareFactory.CSRF_NAG_HTTPS, nagHttps,
            CSRFMiddlewareFactory.CSRF_ORIGIN, origin,
            CSRFMiddlewareFactory.CSRF_TIMEOUT_IN_MINUTES, timeoutMins);

        // when
        final ThrowingSupplier<CSRFMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), CSRFMiddlewareOptions.class);

        // then
        final CSRFMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);

        assertNotNull(options.getCookie());
        assertEquals(cookieName, options.getCookie().getName());
        assertEquals(cookiePath, options.getCookie().getPath());
        assertEquals(cookieSecure, options.getCookie().isSecure());

        assertEquals(headerName, options.getHeaderName());
        assertEquals(nagHttps, options.nagHTTPs());
        assertEquals(origin, options.getOrigin());
        assertEquals(timeoutMins, options.getTimeoutMinutes());
    }
}
