package com.inventage.portal.gateway.proxy.middleware.cors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CorsMiddlewareOptions.Builder.class)
public abstract class AbstractCorsMiddlewareOptions implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsMiddlewareOptions.class);

    @JsonProperty(CorsMiddlewareFactory.ALLOWED_ORIGINS)
    public abstract List<String> getAllowedOrigins();

    @JsonProperty(CorsMiddlewareFactory.ALLOWED_ORIGIN_PATTERNS)
    public abstract List<String> getAllowedOriginPatterns();

    @JsonProperty(CorsMiddlewareFactory.ALLOWED_METHODS)
    public abstract Set<HttpMethod> getAllowedMethods();

    @JsonProperty(CorsMiddlewareFactory.ALLOWED_HEADERS)
    public abstract Set<String> getAllowedHeaders();

    @JsonProperty(CorsMiddlewareFactory.EXPOSED_HEADERS)
    public abstract Set<String> getExposedHeaders();

    @Default
    @JsonProperty(CorsMiddlewareFactory.MAX_AGE_SECONDS)
    public int getMaxAgeSeconds() {
        logDefault(LOGGER, CorsMiddlewareFactory.MAX_AGE_SECONDS, CorsMiddlewareFactory.DEFAULT_MAX_AGE_SECONDS);
        return CorsMiddlewareFactory.DEFAULT_MAX_AGE_SECONDS;
    };

    @Default
    @JsonProperty(CorsMiddlewareFactory.ALLOW_CREDENTIALS)
    public boolean allowCredentials() {
        logDefault(LOGGER, CorsMiddlewareFactory.ALLOW_CREDENTIALS, CorsMiddlewareFactory.DEFAULT_ALLOW_CREDENTIALS);
        return CorsMiddlewareFactory.DEFAULT_ALLOW_CREDENTIALS;
    }

    @Default
    @JsonProperty(CorsMiddlewareFactory.ALLOW_PRIVATE_NETWORK)
    public boolean allowPrivateNetworks() {
        logDefault(LOGGER, CorsMiddlewareFactory.ALLOW_PRIVATE_NETWORK, CorsMiddlewareFactory.DEFAULT_ALLOW_PRIVATE_NETWORK);
        return CorsMiddlewareFactory.DEFAULT_ALLOW_PRIVATE_NETWORK;
    }
}
