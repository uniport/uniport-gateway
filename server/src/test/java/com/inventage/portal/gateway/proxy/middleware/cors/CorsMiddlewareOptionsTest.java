package com.inventage.portal.gateway.proxy.middleware.cors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpMethod;
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
        final HttpMethod allowedMethod = HttpMethod.GET;
        final String allowedHeader = "aHeader";
        final String exposedHeader = "aHeader";
        final Integer maxAgeSeconds = 42;
        final Boolean allowCredentials = true;
        final Boolean allowPrivateNetwork = true;

        final JsonObject json = JsonObject.of(
            CorsMiddlewareFactory.CORS_ALLOWED_ORIGINS, List.of(allowedOrigin),
            CorsMiddlewareFactory.CORS_ALLOWED_ORIGIN_PATTERNS, List.of(allowedOriginPattern),
            CorsMiddlewareFactory.CORS_ALLOWED_METHODS, List.of(allowedMethod.toString()),
            CorsMiddlewareFactory.CORS_ALLOWED_HEADERS, List.of(allowedHeader),
            CorsMiddlewareFactory.CORS_EXPOSED_HEADERS, List.of(exposedHeader),
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
        assertTrue(options.getAllowedMethods().contains(allowedMethod));

        assertNotNull(options.getAllowedHeaders());
        assertTrue(options.getAllowedHeaders().contains(allowedHeader));

        assertNotNull(options.getExposedHeaders());
        assertTrue(options.getExposedHeaders().contains(exposedHeader));

        assertEquals(maxAgeSeconds, options.getMaxAgeSeconds());
        assertEquals(allowCredentials, options.allowCredentials());
        assertEquals(allowPrivateNetwork, options.allowPrivateNetworks());
    }
}
