package ch.uniport.gateway.proxy.middleware.claimToHeader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ClaimToHeaderMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String name = "aName";
        final String path = "aPath";

        final JsonObject json = JsonObject.of(
            ClaimToHeaderMiddlewareFactory.NAME, name,
            ClaimToHeaderMiddlewareFactory.PATH, path);

        // when
        final ThrowingSupplier<ClaimToHeaderMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ClaimToHeaderMiddlewareOptions.class);

        // then
        final ClaimToHeaderMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(name, options.getName());
        assertEquals(path, options.getPath());
    }
}
