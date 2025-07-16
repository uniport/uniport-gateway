package ch.uniport.gateway.proxy.middleware.controlApi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.uniport.gateway.proxy.middleware.controlapi.ControlApiAction;
import ch.uniport.gateway.proxy.middleware.controlapi.ControlApiMiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.controlapi.ControlApiMiddlewareOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ControlApiMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final ControlApiAction action = ControlApiAction.SESSION_TERMINATE;
        final String sessionResetURL = "anURL";

        final JsonObject json = JsonObject.of(
            ControlApiMiddlewareFactory.ACTION, action,
            ControlApiMiddlewareFactory.SESSION_RESET_URL, sessionResetURL);

        // when
        final ThrowingSupplier<ControlApiMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ControlApiMiddlewareOptions.class);

        // then
        final ControlApiMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(action, options.getAction());
        assertEquals(sessionResetURL, options.getSessionResetURL());
    }
}
