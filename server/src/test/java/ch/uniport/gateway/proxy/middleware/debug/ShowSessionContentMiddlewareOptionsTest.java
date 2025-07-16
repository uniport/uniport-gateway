package ch.uniport.gateway.proxy.middleware.debug;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ShowSessionContentMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final JsonObject json = JsonObject.of();

        // when
        final ThrowingSupplier<ShowSessionContentMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ShowSessionContentMiddlewareOptions.class);

        // then
        final ShowSessionContentMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
    }
}
