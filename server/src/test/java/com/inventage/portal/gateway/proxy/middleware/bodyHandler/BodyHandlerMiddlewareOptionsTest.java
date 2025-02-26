package com.inventage.portal.gateway.proxy.middleware.bodyHandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class BodyHandlerMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final JsonObject json = JsonObject.of();

        // when
        final ThrowingSupplier<BodyHandlerMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), BodyHandlerMiddlewareOptions.class);

        // then
        final BodyHandlerMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
    }
}
