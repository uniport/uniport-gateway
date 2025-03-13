package com.inventage.portal.gateway.proxy.middleware.cors;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link CorsMiddleware}.
 */
public class CorsMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "cors";
    public static final String ALLOWED_ORIGINS = "allowedOrigins";
    public static final String ALLOWED_ORIGIN_PATTERNS = "allowedOriginPatterns";
    public static final String ALLOWED_METHODS = "allowedMethods";
    public static final String ALLOWED_HEADERS = "allowedHeaders";
    public static final String EXPOSED_HEADERS = "exposedHeaders";
    public static final String MAX_AGE_SECONDS = "maxAgeSeconds";
    public static final String ALLOW_CREDENTIALS = "allowCredentials";
    public static final String ALLOW_PRIVATE_NETWORK = "allowPrivateNetwork";

    public static final int DEFAULT_MAX_AGE_SECONDS = -1;
    public static final boolean DEFAULT_ALLOW_CREDENTIALS = false;
    public static final boolean DEFAULT_ALLOW_PRIVATE_NETWORK = false;

    private static final String ORIGIN_LOCALHOST = "http://localhost";
    private static final String[] HTTP_METHODS = new String[] {
        HttpMethod.GET.toString(),
        HttpMethod.HEAD.toString(),
        HttpMethod.POST.toString(),
        HttpMethod.PUT.toString(),
        HttpMethod.DELETE.toString(),
        HttpMethod.PATCH.toString(),
        HttpMethod.OPTIONS.toString(),
        HttpMethod.TRACE.toString(),
        HttpMethod.CONNECT.toString()
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(CorsMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(ALLOWED_ORIGINS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(ALLOWED_ORIGIN_PATTERNS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(ALLOWED_METHODS, Schemas.arraySchema()
                .items(Schemas.enumSchema((Object[]) HTTP_METHODS)))
            .optionalProperty(ALLOWED_HEADERS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(EXPOSED_HEADERS, Schemas.arraySchema()
                .items(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(MAX_AGE_SECONDS, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0)))
            .optionalProperty(ALLOW_CREDENTIALS, Schemas.booleanSchema()
                .defaultValue(DEFAULT_ALLOW_CREDENTIALS))
            .optionalProperty(ALLOW_PRIVATE_NETWORK, Schemas.booleanSchema()
                .defaultValue(DEFAULT_ALLOW_PRIVATE_NETWORK))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<CorsMiddlewareOptions> modelType() {
        return CorsMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final CorsMiddlewareOptions options = castOptions(config, modelType());

        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new CorsMiddleware(
                name,
                allowedOrigins(options.getAllowedOrigins()),
                options.getAllowedOriginPatterns(),
                options.getAllowedMethods(),
                options.getAllowedHeaders(),
                options.getExposedHeaders(),
                options.getMaxAgeSeconds(),
                options.allowCredentials(),
                options.allowPrivateNetworks()));
    }

    /**
     * Allowed origins always contains the 'http;//localhost' origin
     * 
     * @param origins
     * @return
     */
    private List<String> allowedOrigins(List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            return List.of(ORIGIN_LOCALHOST);
        }

        origins = new LinkedList<>(origins);
        if (!origins.contains(ORIGIN_LOCALHOST)) {
            origins.add(ORIGIN_LOCALHOST);
        }

        return origins;
    }
}
