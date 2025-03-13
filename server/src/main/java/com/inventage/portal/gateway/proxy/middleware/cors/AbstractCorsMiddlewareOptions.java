package com.inventage.portal.gateway.proxy.middleware.cors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = CorsMiddlewareOptions.Builder.class)
public abstract class AbstractCorsMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsMiddlewareOptions.class);

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_ORIGINS)
    public abstract List<String> getAllowedOrigins();

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_ORIGIN_PATTERNS)
    public abstract List<String> getAllowedOriginPatterns();

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_METHODS)
    public abstract Set<HttpMethod> getAllowedMethods();

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_HEADERS)
    public abstract Set<String> getAllowedHeaders();

    @JsonProperty(CorsMiddlewareFactory.CORS_EXPOSED_HEADERS)
    public abstract Set<String> getExposedHeaders();

    @Default
    @JsonProperty(CorsMiddlewareFactory.CORS_MAX_AGE_SECONDS)
    public int getMaxAgeSeconds() {
        logDefault(LOGGER, CorsMiddlewareFactory.CORS_MAX_AGE_SECONDS, CorsMiddlewareFactory.DEFAULT_MAX_AGE_SECONDS);
        return CorsMiddlewareFactory.DEFAULT_MAX_AGE_SECONDS;
    };

    @Default
    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOW_CREDENTIALS)
    public boolean allowCredentials() {
        logDefault(LOGGER, CorsMiddlewareFactory.CORS_ALLOW_CREDENTIALS, CorsMiddlewareFactory.DEFAULT_ALLOW_CREDENTIALS);
        return CorsMiddlewareFactory.DEFAULT_ALLOW_CREDENTIALS;
    }

    @Default
    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOW_PRIVATE_NETWORK)
    public boolean allowPrivateNetworks() {
        logDefault(LOGGER, CorsMiddlewareFactory.CORS_ALLOW_PRIVATE_NETWORK, CorsMiddlewareFactory.DEFAULT_ALLOW_PRIVATE_NETWORK);
        return CorsMiddlewareFactory.DEFAULT_ALLOW_PRIVATE_NETWORK;
    }
}
