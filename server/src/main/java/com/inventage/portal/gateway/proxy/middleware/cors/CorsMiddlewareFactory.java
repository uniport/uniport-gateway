package com.inventage.portal.gateway.proxy.middleware.cors;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link CorsMiddleware}.
 */
public class CorsMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String CORS = "cors";
    public static final String CORS_ALLOWED_ORIGINS = "allowedOrigins";
    public static final String CORS_ALLOWED_ORIGIN_PATTERNS = "allowedOriginPatterns";
    public static final String CORS_ALLOWED_METHODS = "allowedMethods";
    public static final String CORS_ALLOWED_HEADERS = "allowedHeaders";
    public static final String CORS_EXPOSED_HEADERS = "exposedHeaders";
    public static final String CORS_MAX_AGE_SECONDS = "maxAgeSeconds";
    public static final String CORS_ALLOW_CREDENTIALS = "allowCredentials";
    public static final String CORS_ALLOW_PRIVATE_NETWORK = "allowPrivateNetwork";

    public static final int DEFAULT_MAX_AGE_SECONDS = -1;
    public static final boolean DEFAULT_ALLOW_CREDENTIALS = false;
    public static final boolean DEFAULT_ALLOW_PRIVATE_NETWORKS = false;

    private static final String ORIGIN_LOCALHOST = "http://localhost";
    private static final String[] HTTP_METHODS = new String[] {
        "GET",
        "HEAD",
        "POST",
        "PUT",
        "DELETE",
        "PATCH",
        "OPTIONS",
        "TRACE",
        "CONNECT"
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(CorsMiddlewareFactory.class);

    @Override
    public String provides() {
        return CORS;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(CORS_ALLOWED_ORIGINS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(CORS_ALLOWED_ORIGIN_PATTERNS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(CORS_ALLOWED_METHODS, Schemas.arraySchema()
                .items(Schemas.enumSchema((Object[]) HTTP_METHODS)))
            .optionalProperty(CORS_ALLOWED_HEADERS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(CORS_EXPOSED_HEADERS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(CORS_MAX_AGE_SECONDS, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0)))
            .optionalProperty(CORS_ALLOW_CREDENTIALS, Schemas.booleanSchema()
                .defaultValue(DEFAULT_ALLOW_CREDENTIALS))
            .optionalProperty(CORS_ALLOW_PRIVATE_NETWORK, Schemas.booleanSchema()
                .defaultValue(DEFAULT_ALLOW_PRIVATE_NETWORKS))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, CORS_ALLOWED_ORIGINS, allowedOrigins(null));
        logDefaultIfNotConfigured(LOGGER, options, CORS_ALLOWED_ORIGIN_PATTERNS, allowedOriginPatterns(null));
        logDefaultIfNotConfigured(LOGGER, options, CORS_ALLOWED_METHODS, allowedMethods(null));
        logDefaultIfNotConfigured(LOGGER, options, CORS_ALLOWED_HEADERS, toSet(null));
        logDefaultIfNotConfigured(LOGGER, options, CORS_EXPOSED_HEADERS, toSet(null));
        logDefaultIfNotConfigured(LOGGER, options, CORS_MAX_AGE_SECONDS, DEFAULT_MAX_AGE_SECONDS);
        logDefaultIfNotConfigured(LOGGER, options, CORS_ALLOW_CREDENTIALS, DEFAULT_ALLOW_CREDENTIALS);
        logDefaultIfNotConfigured(LOGGER, options, CORS_ALLOW_PRIVATE_NETWORK, DEFAULT_ALLOW_PRIVATE_NETWORKS);

        return Future.succeededFuture();
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return CorsMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {

        final JsonArray allowedOrigin = middlewareConfig.getJsonArray(CORS_ALLOWED_ORIGINS);
        final JsonArray allowedOriginPatterns = middlewareConfig.getJsonArray(CORS_ALLOWED_ORIGIN_PATTERNS);

        final JsonArray allowedMethods = middlewareConfig.getJsonArray(CORS_ALLOWED_METHODS);
        final JsonArray allowedHeaders = middlewareConfig.getJsonArray(CORS_ALLOWED_HEADERS);
        final JsonArray exposedHeaders = middlewareConfig.getJsonArray(CORS_EXPOSED_HEADERS);

        final int maxAgeSeconds = middlewareConfig.getInteger(CORS_MAX_AGE_SECONDS, DEFAULT_MAX_AGE_SECONDS);
        final boolean allowCredentials = middlewareConfig.getBoolean(CORS_ALLOW_CREDENTIALS, DEFAULT_ALLOW_CREDENTIALS);
        final boolean allowPrivateNetwork = middlewareConfig.getBoolean(CORS_ALLOW_PRIVATE_NETWORK, DEFAULT_ALLOW_PRIVATE_NETWORKS);

        LOGGER.info("Created '{}' middleware successfully", CORS);
        return Future.succeededFuture(new CorsMiddleware(
            name,
            allowedOrigins(allowedOrigin),
            allowedOriginPatterns(allowedOriginPatterns),
            allowedMethods(allowedMethods),
            toSet(allowedHeaders),
            toSet(exposedHeaders),
            maxAgeSeconds,
            allowCredentials,
            allowPrivateNetwork));
    }

    /**
     * Allowed origins always contains the 'http;//localhost' origin
     * 
     * @param originsJSON
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<String> allowedOrigins(JsonArray originsJSON) {
        if (originsJSON == null || originsJSON.isEmpty()) {
            return List.of(ORIGIN_LOCALHOST);
        }

        final List<String> origins = originsJSON.getList();
        if (!origins.contains(ORIGIN_LOCALHOST)) {
            origins.add(ORIGIN_LOCALHOST);
        }

        return origins;
    }

    @SuppressWarnings("unchecked")
    private List<String> allowedOriginPatterns(JsonArray originPatterns) {
        if (originPatterns == null || originPatterns.isEmpty()) {
            return List.of();
        }
        return originPatterns.getList();
    }

    @SuppressWarnings("unchecked")
    private Set<HttpMethod> allowedMethods(JsonArray methods) {
        if (methods == null || methods.isEmpty()) {
            return Set.of();
        }

        return (Set<HttpMethod>) methods.getList().stream()
            .map(m -> HttpMethod.valueOf((String) m))
            .collect(Collectors.<HttpMethod>toSet());
    }

    @SuppressWarnings("unchecked")
    private Set<String> toSet(JsonArray list) {
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        return new HashSet<String>(list.getList());
    }

}
