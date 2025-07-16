package ch.uniport.gateway.proxy.middleware.authorization.authorizationBearer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class AuthorizationBearerMiddlewareOptionsTest {

    @Test
    public void shouldCreateFromBuilder() {
        // given
        final String sessionScope = "aSessionScope";

        // when
        final AuthorizationBearerMiddlewareOptions options = AuthorizationBearerMiddlewareOptions.builder()
            .withSessionScope(sessionScope)
            .build();

        // then
        assertEquals(sessionScope, options.getSessionScope());
    }

    @Test
    public void shouldCreateFromJson() {
        // given
        final String sessionScope = "aSessionScope";
        final JsonObject json = JsonObject.of(
            AuthorizationBearerMiddlewareFactory.SESSION_SCOPE, sessionScope);

        // when
        final ThrowingSupplier<AuthorizationBearerMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), AuthorizationBearerMiddlewareOptions.class);

        // then
        final AuthorizationBearerMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(sessionScope, options.getSessionScope());
    }
}
