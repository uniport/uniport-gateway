package com.inventage.portal.gateway.proxy.middleware.oauth2.foreignInitiated;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class PreventForeignInitiatedAuthMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String redirectURI = "anURI";

        final JsonObject json = JsonObject.of(
            PreventForeignInitiatedAuthMiddlewareFactory.PREVENT_FOREIGN_INITIATED_AUTHENTICATION_REDIRECT, redirectURI);

        // when
        final ThrowingSupplier<PreventForeignInitiatedAuthMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), PreventForeignInitiatedAuthMiddlewareOptions.class);

        // then
        final PreventForeignInitiatedAuthMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(redirectURI, options.getRedirectURI());
    }
}
