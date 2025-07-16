package ch.uniport.gateway.proxy.middleware.log;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class RequestResponseLoggerMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String filterRegex = "aFilterRegex";
        final String contentType = "aContentType";
        final Boolean requestEnabled = true;
        final Boolean responseEnabled = true;

        final JsonObject json = JsonObject.of(
            RequestResponseLoggerMiddlewareFactory.FILTER_REGEX, filterRegex,
            RequestResponseLoggerMiddlewareFactory.CONTENT_TYPES, List.of(contentType),
            RequestResponseLoggerMiddlewareFactory.LOGGING_REQUEST_ENABLED, requestEnabled,
            RequestResponseLoggerMiddlewareFactory.LOGGING_RESPONSE_ENABLED, responseEnabled);

        // when
        final ThrowingSupplier<RequestResponseLoggerMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), RequestResponseLoggerMiddlewareOptions.class);

        // then
        final RequestResponseLoggerMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(filterRegex, options.getFilterRegex());
        assertNotNull(options.getContentTypes());
        assertEquals(contentType, options.getContentTypes().get(0));
        assertEquals(requestEnabled, options.isRequestEnabled());
        assertEquals(responseEnabled, options.isResponseEnabled());
    }
}
