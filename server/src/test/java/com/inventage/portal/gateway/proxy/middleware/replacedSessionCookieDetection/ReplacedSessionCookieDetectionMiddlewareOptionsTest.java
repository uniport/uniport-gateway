package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ReplacedSessionCookieDetectionMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String cookieName = "aCookieName";
        final Integer waitBeforeRetry = 42;
        final Integer maxRetries = 3;

        final JsonObject json = JsonObject.of(
            ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME, cookieName,
            ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS, waitBeforeRetry,
            ReplacedSessionCookieDetectionMiddlewareFactory.REPLACED_SESSION_COOKIE_DETECTION_MAX_REDIRECT_RETRIES, maxRetries);

        // when
        final ThrowingSupplier<ReplacedSessionCookieDetectionMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ReplacedSessionCookieDetectionMiddlewareOptions.class);

        // then
        final ReplacedSessionCookieDetectionMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(cookieName, options.getCookieName());
        assertEquals(waitBeforeRetry, options.getWaitBeforeRetryMs());
        assertEquals(maxRetries, options.getMaxRetries());
    }
}
