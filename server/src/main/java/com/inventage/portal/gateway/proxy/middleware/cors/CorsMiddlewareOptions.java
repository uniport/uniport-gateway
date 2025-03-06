package com.inventage.portal.gateway.proxy.middleware.cors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = CorsMiddlewareOptions.Builder.class)
public final class CorsMiddlewareOptions implements GatewayMiddlewareOptions {

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

    public static Builder builder() {
        return new Builder();
    }

    private CorsMiddlewareOptions(Builder builder) {
        this.allowedOriginPatterns = builder.allowedOriginPatterns;
        this.allowedMethods = builder.allowedMethods;
        this.allowedHeaders = builder.allowedHeaders;
        this.exposedHeaders = builder.exposedHeaders;
        this.maxAgeSeconds = builder.maxAgeSeconds;
        this.allowCredentials = builder.allowCredentials;
        this.allowPrivateNetworks = builder.allowPrivateNetworks;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private List<String> allowedOrigins;
        private List<String> allowedOriginPatterns;
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private List<String> exposedHeaders;
        private Integer maxAgeSeconds;
        private Boolean allowCredentials;
        private Boolean allowPrivateNetworks;

        public Builder withAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
            return this;
        }

        public Builder withAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
            return this;
        }

        public Builder withAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
            return this;
        }

        public Builder withAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
            return this;
        }

        public Builder withExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
            return this;
        }

        public Builder withMaxAgeSeconds(Integer maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
            return this;
        }

        public Builder withAllowCredentials(Boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
            return this;
        }

        public Builder withAllowPrivateNetworks(Boolean allowPrivateNetworks) {
            this.allowPrivateNetworks = allowPrivateNetworks;
            return this;
        }

        public CorsMiddlewareOptions build() {
            return new CorsMiddlewareOptions(this);
        }
    }
}
