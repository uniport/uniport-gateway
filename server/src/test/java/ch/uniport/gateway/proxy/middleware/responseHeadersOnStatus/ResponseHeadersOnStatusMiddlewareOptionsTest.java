package ch.uniport.gateway.proxy.middleware.responseHeadersOnStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ResponseHeadersOnStatusMiddlewareOptionsTest {

    @Test
    public void shouldParseSetResponseHeaders() {
        // given
        final int statusCode = 401;
        final String headerName = "WWW-Authenticate";
        final String headerValue = "Bearer realm=\"example\"";

        final JsonObject json = JsonObject.of(
            ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, statusCode,
            ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS, Map.of(
                headerName, headerValue));

        // when
        final ThrowingSupplier<ResponseHeadersOnStatusMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ResponseHeadersOnStatusMiddlewareOptions.class);

        // then
        final ResponseHeadersOnStatusMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(statusCode, options.getStatusCode());

        assertNotNull(options.getSetResponseHeaders());
        assertEquals(headerValue, options.getSetResponseHeaders().get(headerName));

        assertNull(options.getRewriteResponseHeaders());
    }

    @Test
    public void shouldParseRewriteResponseHeaders() {
        // given
        final int statusCode = 401;
        final String headerName = "WWW-Authenticate";
        final String regex = "(resource_metadata=\"[^\"]*\\.well-known/oauth-protected-resource)(\")";
        final String replacement = "$1/organisation/mcp$2";

        final JsonObject json = JsonObject.of(
            ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, statusCode,
            ResponseHeadersOnStatusMiddlewareFactory.REWRITE_RESPONSE_HEADERS, Map.of(
                headerName, Map.of(
                    ResponseHeadersOnStatusMiddlewareFactory.REGEX, regex,
                    ResponseHeadersOnStatusMiddlewareFactory.REPLACEMENT, replacement)));

        // when
        final ThrowingSupplier<ResponseHeadersOnStatusMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ResponseHeadersOnStatusMiddlewareOptions.class);

        // then
        final ResponseHeadersOnStatusMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(statusCode, options.getStatusCode());

        assertNull(options.getSetResponseHeaders());

        assertNotNull(options.getRewriteResponseHeaders());
        RewriteRule rule = options.getRewriteResponseHeaders().get(headerName);
        assertNotNull(rule);
        assertEquals(regex, rule.getRegex());
        assertEquals(replacement, rule.getReplacement());
    }

    @Test
    public void shouldParseCombinedOptions() {
        // given
        final int statusCode = 401;

        final JsonObject json = JsonObject.of(
            ResponseHeadersOnStatusMiddlewareFactory.STATUS_CODE, statusCode,
            ResponseHeadersOnStatusMiddlewareFactory.SET_RESPONSE_HEADERS, Map.of(
                "X-Custom", "value"),
            ResponseHeadersOnStatusMiddlewareFactory.REWRITE_RESPONSE_HEADERS, Map.of(
                "WWW-Authenticate", Map.of(
                    ResponseHeadersOnStatusMiddlewareFactory.REGEX, "old",
                    ResponseHeadersOnStatusMiddlewareFactory.REPLACEMENT, "new")));

        // when
        final ThrowingSupplier<ResponseHeadersOnStatusMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ResponseHeadersOnStatusMiddlewareOptions.class);

        // then
        final ResponseHeadersOnStatusMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(statusCode, options.getStatusCode());
        assertNotNull(options.getSetResponseHeaders());
        assertNotNull(options.getRewriteResponseHeaders());
    }
}
