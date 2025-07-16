package ch.uniport.gateway.proxy.middleware.openTelemetry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class OpenTelemetryMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final JsonObject json = JsonObject.of();

        // when
        final ThrowingSupplier<OpenTelemetryMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), OpenTelemetryMiddlewareOptions.class);

        // then
        final OpenTelemetryMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
    }
}
