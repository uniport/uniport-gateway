package ch.uniport.gateway.proxy.middleware.sessionBag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class SessionBagMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String sessionCookieName = "aName";
        final String whitelistedCookieName = "aName";
        final String whitelistedCookiePath = "aName";

        final JsonObject json = JsonObject.of(
            SessionBagMiddlewareFactory.SESSION_COOKIE_NAME, sessionCookieName,
            SessionBagMiddlewareFactory.WHITELISTED_COOKIES, List.of(
                Map.of(
                    SessionBagMiddlewareFactory.WHITELISTED_COOKIE_NAME, whitelistedCookieName,
                    SessionBagMiddlewareFactory.WHITELISTED_COOKIE_PATH, whitelistedCookiePath))

        );

        // when
        final ThrowingSupplier<SessionBagMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), SessionBagMiddlewareOptions.class);

        // then
        final SessionBagMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(sessionCookieName, options.getSessionCookieName());

        assertNotNull(options.getWhitelistedCookieOptions());
        assertNotNull(options.getWhitelistedCookieOptions().get(0));
        assertEquals(whitelistedCookieName, options.getWhitelistedCookieOptions().get(0).getName());
        assertEquals(whitelistedCookiePath, options.getWhitelistedCookieOptions().get(0).getPath());

    }

}
