package com.inventage.portal.gateway.proxy.middleware.customResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class CustomResponseMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final Integer statusCode = 418;
        final String headerName = "aName";
        final String headerValue = "aValue";
        final String content = "someContent";

        final JsonObject json = JsonObject.of(
            CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_STATUS_CODE, statusCode,
            CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_HEADERS, Map.of(
                headerName, headerValue),
            CustomResponseMiddlewareFactory.CUSTOM_RESPONSE_CONTENT, content);

        // when
        final ThrowingSupplier<CustomResponseMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), CustomResponseMiddlewareOptions.class);

        // then
        final CustomResponseMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(statusCode, options.getStatusCode());

        assertNotNull(options.getHeaders());
        assertTrue(options.getHeaders().containsKey(headerName));
        assertEquals(headerValue, options.getHeaders().get(headerName));

        assertEquals(content, options.getContent());
    }
}
