package com.inventage.portal.gateway.core.config;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.CompositeFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;

import java.util.ArrayList;
import java.util.List;

/**
 * It defines the structure of the static configuration.
 */
public class StaticConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticConfiguration.class);

    // keywords used for internal purpose only
    public static final String ENTRYPOINTS = "entrypoints";
    public static final String ENTRYPOINT_NAME = "name";
    public static final String ENTRYPOINT_PORT = "port";
    public static final String ENTRYPOINT_SESSION_DISABLED = "sessionDisabled";
    public static final String ENTRYPOINT_SESSION_IDLE_TIMEOUT = "sessionIdleTimeout";

    public static final String APPLICATIONS = "applications";
    public static final String APPLICATION_NAME = "name";
    public static final String APPLICATION_ENTRYPOINT = "entrypoint";
    // TODO: the following two properties are currently not respected by this application
    public static final String APPLICATION_REQUEST_SELECTOR = "requestSelector";
    public static final String APPLICATION_REQUEST_SELECTOR_URL_PREFIX = "urlPrefix";
    public static final String APPLICATION_PROVIDER = "provider";

    public static final String PROVIDERS = "providers";
    // TODO: the following property is currently not publicly available due to not fitting into the
    // usual provider schema
    public static final String PROVIDERS_THROTTLE_INTERVAL_MS = "providersThrottleIntervalMs";
    public static final String PROVIDER_NAME = "name";

    public static final String PROVIDER_FILE = "file";
    public static final String PROVIDER_FILE_FILENAME = "filename";
    public static final String PROVIDER_FILE_DIRECTORY = "directory";
    public static final String PROVIDER_FILE_WATCH = "watch";

    public static final String PROVIDER_DOCKER = "docker";
    public static final String PROVIDER_DOCKER_ENDPOINT = "endpoint";
    public static final String PROVIDER_DOCKER_EXPOSED_BY_DEFAULT = "exposedByDefault";
    public static final String PROVIDER_DOCKER_NETWORK = "network";
    public static final String PROVIDER_DOCKER_DEFAULT_RULE = "defaultRule";

    public static final String PROVIDER_KUBERNETES = "kubernetesIngress";

    private static Schema schema;

    private static Schema buildSchema(Vertx vertx) {
        ObjectSchemaBuilder entrypointSchema = Schemas.objectSchema()
                .requiredProperty(ENTRYPOINT_NAME, Schemas.stringSchema())
                .requiredProperty(ENTRYPOINT_PORT, Schemas.intSchema())
                .property(ENTRYPOINT_SESSION_DISABLED, Schemas.booleanSchema())
                .property(DynamicConfiguration.MIDDLEWARES, Schemas.arraySchema().items(DynamicConfiguration.getBuildMiddlewareSchema()))
                .property(ENTRYPOINT_SESSION_IDLE_TIMEOUT, Schemas.intSchema()).allowAdditionalProperties(false);

        ObjectSchemaBuilder applicationSchema = Schemas.objectSchema()
                .requiredProperty(APPLICATION_NAME, Schemas.stringSchema())
                .requiredProperty(APPLICATION_ENTRYPOINT, Schemas.stringSchema())
                .requiredProperty(APPLICATION_REQUEST_SELECTOR,
                        Schemas.objectSchema().requiredProperty(APPLICATION_REQUEST_SELECTOR_URL_PREFIX,
                                Schemas.stringSchema()))
                .requiredProperty(APPLICATION_PROVIDER, Schemas.stringSchema()).allowAdditionalProperties(false);

        ObjectSchemaBuilder providerSchema = Schemas.objectSchema()
                .requiredProperty(PROVIDER_NAME, Schemas.stringSchema())
                .property(PROVIDER_FILE_FILENAME, Schemas.stringSchema())
                .property(PROVIDER_FILE_DIRECTORY, Schemas.stringSchema())
                .property(PROVIDER_FILE_WATCH, Schemas.booleanSchema())
                .property(PROVIDER_DOCKER_ENDPOINT, Schemas.stringSchema())
                .property(PROVIDER_DOCKER_EXPOSED_BY_DEFAULT, Schemas.booleanSchema())
                .property(PROVIDER_DOCKER_NETWORK, Schemas.stringSchema())
                .property(PROVIDER_DOCKER_DEFAULT_RULE, Schemas.stringSchema()).allowAdditionalProperties(false);

        ObjectSchemaBuilder staticConfigBuilder = Schemas.objectSchema()
                .property(ENTRYPOINTS, Schemas.arraySchema().items(entrypointSchema))
                .property(APPLICATIONS, Schemas.arraySchema().items(applicationSchema))
                .property(PROVIDERS, Schemas.arraySchema().items(providerSchema));

        SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createDraft201909SchemaParser(schemaRouter);
        return staticConfigBuilder.build(schemaParser);
    }

    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        if (schema == null) {
            schema = buildSchema(vertx);
        }

        Promise<Void> validPromise = Promise.promise();
        schema.validateAsync(json).onSuccess(f -> {
            List<Future> futures = validateEntrypoints(json.getJsonArray(ENTRYPOINTS));
            futures.add(validateProviders(json.getJsonArray(PROVIDERS)));

            CompositeFuture.all(futures).onSuccess(cf -> {
                validPromise.complete();
            }).onFailure(cfErr -> {
                validPromise.fail(cfErr.getMessage());
            });
        }).onFailure(err -> {
            validPromise.fail(err.getMessage());
        });
        return validPromise.future();
    }

    private static List<Future> validateEntrypoints(JsonArray entrypoints) {
        List<Future> middlewareFutures = new ArrayList<>();
        if (entrypoints != null) {
            for (int i = 0; i < entrypoints.size(); i++) {
                JsonObject entrypointJson = entrypoints.getJsonObject(i);
                JsonArray middlewares = entrypointJson.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                middlewareFutures.add(validateEntryMiddlewareFuture(middlewares));
            }
        }
        return middlewareFutures;
    }

    private static Future<Void> validateEntryMiddlewareFuture(JsonArray entryMiddleware) {
        JsonObject toValidate = new JsonObject().put(DynamicConfiguration.MIDDLEWARES, entryMiddleware);
        return DynamicConfiguration.validateMiddlewares(toValidate);
    }

    private static Future<Void> validateProviders(JsonArray providers) {
        if (providers == null || providers.size() == 0) {
            LOGGER.warn("No providers defined");
            return Future.succeededFuture();
        }
        LOGGER.debug("Validating providers: '{}'", providers);

        for (int i = 0; i < providers.size(); i++) {
            JsonObject provider = providers.getJsonObject(i);
            String providerName = provider.getString(PROVIDER_NAME);

            Boolean valid = true;
            String errMsg = "";
            switch (providerName) {
                case PROVIDER_FILE: {
                    String filename = provider.getString(PROVIDER_FILE_FILENAME);
                    String directory = provider.getString(PROVIDER_FILE_DIRECTORY);
                    if ((filename == null || filename.length() == 0)
                            && (directory == null || directory.length() == 0)) {
                        errMsg = String.format("%s: either filename or directory has to be defined", providerName);
                        valid = false;
                    }
                    break;
                }
                case PROVIDER_DOCKER: {
                    break;
                }
                default: {
                    errMsg = "Unknown provider";
                    valid = false;
                }
            }

            if (!valid) {
                return Future.failedFuture(errMsg);
            }
        }
        return Future.succeededFuture();
    }
}
