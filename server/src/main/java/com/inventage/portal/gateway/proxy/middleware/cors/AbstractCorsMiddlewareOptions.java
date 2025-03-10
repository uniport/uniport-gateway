package com.inventage.portal.gateway.proxy.middleware.cors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import java.util.List;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonDeserialize(builder = CorsMiddlewareOptions.Builder.class)
public abstract class AbstractCorsMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_ORIGINS)
    public abstract List<String> getAllowedOrigins();

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_ORIGIN_PATTERNS)
    public abstract List<String> getAllowedOriginPatterns();

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_METHODS)
    public abstract List<String> getAllowedMethods();

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_HEADERS)
    public abstract List<String> getAllowedHeaders();

    @JsonProperty(CorsMiddlewareFactory.CORS_EXPOSED_HEADERS)
    public abstract List<String> getExposedHeaders();

    @Default
    @JsonProperty(CorsMiddlewareFactory.CORS_MAX_AGE_SECONDS)
    public int getMaxAgeSeconds() {
        return CorsMiddlewareFactory.DEFAULT_MAX_AGE_SECONDS;
    };

    @Default
    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOW_CREDENTIALS)
    public boolean allowCredentials() {
        return CorsMiddlewareFactory.DEFAULT_ALLOW_CREDENTIALS;
    }

    @Default
    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOW_PRIVATE_NETWORK)
    public boolean allowPrivateNetworks() {
        return CorsMiddlewareFactory.DEFAULT_ALLOW_PRIVATE_NETWORKS;
    }
}
