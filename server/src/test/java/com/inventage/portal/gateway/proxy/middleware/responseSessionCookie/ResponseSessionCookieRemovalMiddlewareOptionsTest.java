package com.inventage.portal.gateway.proxy.middleware.responseSessionCookie;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ResponseSessionCookieRemovalMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String sessionCookieName = "aSessionCookieName";
        final JsonObject json = JsonObject.of(
            ResponseSessionCookieRemovalMiddlewareFactory.RESPONSE_SESSION_COOKIE_REMOVAL_NAME, sessionCookieName);

        // when
        final ThrowingSupplier<ResponseSessionCookieRemovalMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ResponseSessionCookieRemovalMiddlewareOptions.class);

        // then
        final ResponseSessionCookieRemovalMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(sessionCookieName, options.getSessionCookieName());
    }
}
