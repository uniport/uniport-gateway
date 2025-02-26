package com.inventage.portal.gateway.proxy.middleware.cors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CorsMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_ORIGINS)
    private List<String> allowedOrigins;

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_ORIGIN_PATTERNS)
    private List<String> allowedOriginPatterns;

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_METHODS)
    private List<String> allowedMethods;

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOWED_HEADERS)
    private List<String> allowedHeaders;

    @JsonProperty(CorsMiddlewareFactory.CORS_EXPOSED_HEADERS)
    private List<String> exposedHeaders;

    @JsonProperty(CorsMiddlewareFactory.CORS_MAX_AGE_SECONDS)
    private Integer maxAgeSeconds;

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOW_CREDENTIALS)
    private Boolean allowCredentials;

    @JsonProperty(CorsMiddlewareFactory.CORS_ALLOW_PRIVATE_NETWORK)
    private Boolean allowPrivateNetworks;

    public CorsMiddlewareOptions() {
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins == null ? null : List.copyOf(allowedOrigins);
    }

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns == null ? null : List.copyOf(allowedOriginPatterns);
    }

    public List<String> getAllowedMethods() {
        return allowedMethods == null ? null : List.copyOf(allowedMethods);
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders == null ? null : List.copyOf(allowedHeaders);
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders == null ? null : List.copyOf(allowedHeaders);
    }

    public Integer getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public Boolean allowCredentials() {
        return allowCredentials;
    }

    public Boolean allowPrivateNetworks() {
        return allowPrivateNetworks;
    }

    @Override
    public CorsMiddlewareOptions clone() {
        try {
            final CorsMiddlewareOptions options = (CorsMiddlewareOptions) super.clone();
            options.allowedOrigins = allowedOrigins == null ? null : List.copyOf(allowedOrigins);
            options.allowedOriginPatterns = allowedOriginPatterns == null ? null : List.copyOf(allowedOriginPatterns);
            options.allowedMethods = allowedMethods == null ? null : List.copyOf(allowedMethods);
            options.allowedHeaders = allowedHeaders == null ? null : List.copyOf(allowedHeaders);
            options.exposedHeaders = exposedHeaders == null ? null : List.copyOf(exposedHeaders);
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
