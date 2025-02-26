package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class RedirectRegexMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String regex = "aRegex";
        final String replacement = "aReplacement";

        final JsonObject json = JsonObject.of(
            RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REGEX, regex,
            RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REPLACEMENT, replacement);

        // when
        final ThrowingSupplier<RedirectRegexMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), RedirectRegexMiddlewareOptions.class);

        // then
        final RedirectRegexMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(regex, options.getRegex());
        assertEquals(replacement, options.getReplacement());
    }
}
