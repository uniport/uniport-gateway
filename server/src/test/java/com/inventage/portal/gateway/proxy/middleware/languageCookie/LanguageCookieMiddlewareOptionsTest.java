package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class LanguageCookieMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String cookieName = "aCookieName";

        final JsonObject json = JsonObject.of(
            LanguageCookieMiddlewareFactory.NAME, cookieName);

        // when
        final ThrowingSupplier<LanguageCookieMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), LanguageCookieMiddlewareOptions.class);

        // then
        final LanguageCookieMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(cookieName, options.getCookieName());
    }
}
