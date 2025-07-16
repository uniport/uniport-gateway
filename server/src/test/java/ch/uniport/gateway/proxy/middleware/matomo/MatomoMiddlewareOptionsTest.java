package ch.uniport.gateway.proxy.middleware.matomo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class MatomoMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String usernameJWTPath = "aUsernameJWTPath";
        final String emailJWTPath = "anEmailJWTPath";
        final String rolesJWTPath = "aRoleJWTPath";
        final String groupJWTPath = "aGroupJWTPath";

        final JsonObject json = JsonObject.of(
            MatomoMiddlewareFactory.JWT_PATH_USERNAME, usernameJWTPath,
            MatomoMiddlewareFactory.JWT_PATH_EMAIL, emailJWTPath,
            MatomoMiddlewareFactory.JWT_PATH_ROLES, rolesJWTPath,
            MatomoMiddlewareFactory.JWT_PATH_GROUP, groupJWTPath);

        // when
        final ThrowingSupplier<MatomoMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), MatomoMiddlewareOptions.class);

        // then
        final MatomoMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(usernameJWTPath, options.getJWTPathUsername());
        assertEquals(emailJWTPath, options.getJWTPathEMail());
        assertEquals(rolesJWTPath, options.getJWTPathRoles());
        assertEquals(groupJWTPath, options.getJWTPathGroup());
    }
}
