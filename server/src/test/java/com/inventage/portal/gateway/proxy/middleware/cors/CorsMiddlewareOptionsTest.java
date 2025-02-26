package com.inventage.portal.gateway.proxy.middleware.cors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class CorsMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String allowedOrigin = "anOrigin";
        final String allowedOriginPattern = "anOriginPattern";
        final String allowedMethods = "aMethod";
        final String allowedHeaders = "aHeader";
        final String exposedHeaders = "aHeader";
        final Integer maxAgeSeconds = 42;
        final Boolean allowCredentials = true;
        final Boolean allowPrivateNetwork = true;

        final JsonObject json = JsonObject.of(
            CorsMiddlewareFactory.CORS_ALLOWED_ORIGINS, List.of(allowedOrigin),
            CorsMiddlewareFactory.CORS_ALLOWED_ORIGIN_PATTERNS, List.of(allowedOriginPattern),
            CorsMiddlewareFactory.CORS_ALLOWED_METHODS, List.of(allowedMethods),
            CorsMiddlewareFactory.CORS_ALLOWED_HEADERS, List.of(allowedHeaders),
            CorsMiddlewareFactory.CORS_EXPOSED_HEADERS, List.of(exposedHeaders),
            CorsMiddlewareFactory.CORS_MAX_AGE_SECONDS, maxAgeSeconds,
            CorsMiddlewareFactory.CORS_ALLOW_CREDENTIALS, allowCredentials,
            CorsMiddlewareFactory.CORS_ALLOW_PRIVATE_NETWORK, allowPrivateNetwork);

        // when
        final ThrowingSupplier<CorsMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), CorsMiddlewareOptions.class);

        // then
        final CorsMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);

        assertNotNull(options.getAllowedOrigins());
        assertEquals(allowedOrigin, options.getAllowedOrigins().get(0));

        assertNotNull(options.getAllowedOriginPatterns());
        assertEquals(allowedOriginPattern, options.getAllowedOriginPatterns().get(0));

        assertNotNull(options.getAllowedMethods());
        assertEquals(allowedMethods, options.getAllowedMethods().get(0));

        assertNotNull(options.getAllowedHeaders());
        assertEquals(allowedHeaders, options.getAllowedHeaders().get(0));

        assertNotNull(options.getExposedHeaders());
        assertEquals(exposedHeaders, options.getExposedHeaders().get(0));

        assertEquals(maxAgeSeconds, options.getMaxAgeSeconds());
        assertEquals(allowCredentials, options.allowCredentials());
        assertEquals(allowPrivateNetwork, options.allowPrivateNetworks());
    }
}
