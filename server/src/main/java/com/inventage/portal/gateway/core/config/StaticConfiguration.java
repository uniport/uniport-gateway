package com.inventage.portal.gateway.core.config;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.Validator;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String APPLICATIONS = "applications";
    public static final String APPLICATION_NAME = "name";
    public static final String APPLICATION_ENTRYPOINT = "entrypoint";
    public static final String ENTRYPOINT_SESSION_DISABLED = "sessionDisabled";
    public static final String ENTRYPOINT_SESSION_IDLE_TIMEOUT = "sessionIdleTimeout";
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

    private static Validator validator;

    private StaticConfiguration() {
    }

    private static Validator buildValidator(Vertx vertx) {
        final ObjectSchemaBuilder entrypointSchema = Schemas.objectSchema()
                .requiredProperty(ENTRYPOINT_NAME, Schemas.stringSchema())
                .requiredProperty(ENTRYPOINT_PORT, Schemas.intSchema())
                .property(DynamicConfiguration.MIDDLEWARES,
                        Schemas.arraySchema().items(DynamicConfiguration.getBuildMiddlewareSchema()))
                .property(ENTRYPOINT_SESSION_DISABLED, Schemas.booleanSchema())
                .property(ENTRYPOINT_SESSION_IDLE_TIMEOUT, Schemas.intSchema()).allowAdditionalProperties(false);

        final ObjectSchemaBuilder applicationSchema = Schemas.objectSchema()
                .requiredProperty(APPLICATION_NAME, Schemas.stringSchema())
                .requiredProperty(APPLICATION_ENTRYPOINT, Schemas.stringSchema())
                .requiredProperty(APPLICATION_REQUEST_SELECTOR,
                        Schemas.objectSchema().requiredProperty(APPLICATION_REQUEST_SELECTOR_URL_PREFIX,
                                Schemas.stringSchema()))
                .requiredProperty(APPLICATION_PROVIDER, Schemas.stringSchema()).allowAdditionalProperties(false);

        final ObjectSchemaBuilder providerSchema = Schemas.objectSchema()
                .requiredProperty(PROVIDER_NAME, Schemas.stringSchema())
                .property(PROVIDER_FILE_FILENAME, Schemas.stringSchema())
                .property(PROVIDER_FILE_DIRECTORY, Schemas.stringSchema())
                .property(PROVIDER_FILE_WATCH, Schemas.booleanSchema())
                .property(PROVIDER_DOCKER_ENDPOINT, Schemas.stringSchema())
                .property(PROVIDER_DOCKER_EXPOSED_BY_DEFAULT, Schemas.booleanSchema())
                .property(PROVIDER_DOCKER_NETWORK, Schemas.stringSchema())
                .property(PROVIDER_DOCKER_DEFAULT_RULE, Schemas.stringSchema()).allowAdditionalProperties(false);

        final ObjectSchemaBuilder staticConfigBuilder = Schemas.objectSchema()
                .property(ENTRYPOINTS, Schemas.arraySchema().items(entrypointSchema))
                .property(APPLICATIONS, Schemas.arraySchema().items(applicationSchema))
                .property(PROVIDERS, Schemas.arraySchema().items(providerSchema));

        JsonSchema schema = JsonSchema.of(staticConfigBuilder.toJson());
        JsonSchemaOptions options = new JsonSchemaOptions().setDraft(Draft.DRAFT202012)
                .setBaseUri("http://inventage.com/portal-gateway/static-configuration");
        return Validator.create(schema, options);
    }

    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        if (validator == null) {
            validator = buildValidator(vertx);
        }

        final Promise<Void> validPromise = Promise.promise();
        try {
            OutputUnit result = validator.validate(json);
            if (!result.getValid()) {
                throw result.toException(json);
            }
        }
        catch (SchemaException | ValidationException e) {
            validPromise.fail(e);
            return validPromise.future();
        }

        final List<Future> futures = validateEntrypoints(json.getJsonArray(ENTRYPOINTS));
        futures.add(validateProviders(json.getJsonArray(PROVIDERS)));
        CompositeFuture.all(futures).onSuccess(cf -> {
            validPromise.complete();
        }).onFailure(cfErr -> {
            validPromise.fail(cfErr.getMessage());
        });

        return validPromise.future();
    }

    private static List<Future> validateEntrypoints(JsonArray entrypoints) {
        final List<Future> middlewareFutures = new ArrayList<>();
        if (entrypoints != null) {
            for (int i = 0; i < entrypoints.size(); i++) {
                final JsonObject entrypointJson = entrypoints.getJsonObject(i);
                final JsonArray middlewares = entrypointJson.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                middlewareFutures.add(validateEntryMiddlewareFuture(middlewares));
            }
        }
        return middlewareFutures;
    }

    private static Future<Void> validateEntryMiddlewareFuture(JsonArray entryMiddleware) {
        final JsonObject toValidate = new JsonObject().put(DynamicConfiguration.MIDDLEWARES, entryMiddleware);
        return DynamicConfiguration.validateMiddlewares(toValidate);
    }

    private static Future<Void> validateProviders(JsonArray providers) {
        if (providers == null || providers.size() == 0) {
            LOGGER.warn("No providers defined");
            return Future.succeededFuture();
        }
        LOGGER.debug("Validating providers: '{}'", providers);

        for (int i = 0; i < providers.size(); i++) {
            final JsonObject provider = providers.getJsonObject(i);
            final String providerName = provider.getString(PROVIDER_NAME);

            boolean valid = true;
            String errMsg = "";
            switch (providerName) {
                case PROVIDER_FILE: {
                    final String filename = provider.getString(PROVIDER_FILE_FILENAME);
                    final String directory = provider.getString(PROVIDER_FILE_DIRECTORY);
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
