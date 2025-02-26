package com.inventage.portal.gateway.proxy.middleware.controlapi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            ControlApiMiddlewareFactory.CONTROL_API_ACTION, action,
            ControlApiMiddlewareFactory.CONTROL_API_SESSION_RESET_URL, sessionResetURL);

        // when
        final ThrowingSupplier<ControlApiMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ControlApiMiddlewareOptions.class);

        // then
        final ControlApiMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(action, options.getAction());
        assertEquals(sessionResetURL, options.getSessionResetURL());
    }
}
