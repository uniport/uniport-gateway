package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ReplacePathRegexMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String regex = "aRegex";
        final String replacement = "aReplacement";

        final JsonObject json = JsonObject.of(
            ReplacePathRegexMiddlewareFactory.REGEX, regex,
            ReplacePathRegexMiddlewareFactory.REPLACEMENT, replacement);

        // when
        final ThrowingSupplier<ReplacePathRegexMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ReplacePathRegexMiddlewareOptions.class);

        // then
        final ReplacePathRegexMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(regex, options.getRegex());
        assertEquals(replacement, options.getReplacement());
    }
}
