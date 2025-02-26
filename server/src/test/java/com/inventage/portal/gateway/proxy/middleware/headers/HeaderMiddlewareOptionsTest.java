package com.inventage.portal.gateway.proxy.middleware.headers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class HeaderMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String reqHeaderName = "aReqHeaderName";
        final String reqHeaderValue = "aReqHeaderValue";

        final String reqHeaderNames = "aReqHeaderNames";
        final List<String> reqHeaderValues = List.of("aReqHeaderValue");

        final String respHeaderName = "aRespHeaderName";
        final String respHeaderValue = "aRespHeaderValue";

        final String respHeaderNames = "aRespHeaderNames";
        final List<String> respHeaderValues = List.of("aRespHeaderValue");

        final JsonObject json = JsonObject.of(
            HeaderMiddlewareFactory.HEADERS_REQUEST, Map.of(
                reqHeaderName, reqHeaderValue,
                reqHeaderNames, reqHeaderValues),
            HeaderMiddlewareFactory.HEADERS_RESPONSE, Map.of(
                respHeaderName, respHeaderValue,
                respHeaderNames, respHeaderValues));

        // when
        final ThrowingSupplier<HeaderMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), HeaderMiddlewareOptions.class);

        // then
        final HeaderMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);

        assertNotNull(options.getRequestHeaders());
        assertTrue(options.getRequestHeaders().containsKey(reqHeaderName));
        assertEquals(List.of(reqHeaderValue), options.getRequestHeaders().get(reqHeaderName));

        assertTrue(options.getRequestHeaders().containsKey(reqHeaderNames));
        assertEquals(reqHeaderValues, options.getRequestHeaders().get(reqHeaderNames));

        assertNotNull(options.getResponseHeaders());
        assertTrue(options.getResponseHeaders().containsKey(respHeaderName));
        assertEquals(List.of(respHeaderValue), options.getResponseHeaders().get(respHeaderName));

        assertTrue(options.getResponseHeaders().containsKey(respHeaderNames));
        assertEquals(respHeaderValues, options.getResponseHeaders().get(respHeaderNames));
    }
}
