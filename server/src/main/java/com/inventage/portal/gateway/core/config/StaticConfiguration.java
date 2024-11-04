package com.inventage.portal.gateway.core.config;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It defines the structure of the static configuration.
 */
public class StaticConfiguration {
    // keywords used for internal purpose only
    public static final String ENTRYPOINTS = "entrypoints";
    public static final String ENTRYPOINT_NAME = "name";
    public static final String ENTRYPOINT_PORT = "port";
    public static final String APPLICATIONS = "applications";
    public static final String APPLICATION_NAME = "name";
    public static final String APPLICATION_ENTRYPOINT = "entrypoint";
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
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticConfiguration.class);
    private static Validator validator;
    private static final String KEYWORD_STRING_MIN_LENGTH = "minLength";
    private static final String KEYWORD_INT_MIN = "minimum";
    private static final String KEYWORD_TYPE = "type";
    private static final String KEYWORD_PATTERN = "pattern";
    private static final String INT_TYPE = "integer";
    private static final String STRING_TYPE = "string";
    private static final String ENV_VARIABLE_PATTERN_STRING_TO_INT = "^\\$\\{.*\\}$";
    private static final int NON_EMPTY_STRING_MIN_LENGTH = 1;
    private static final int NON_ZERO_INT_MIN = 1;

    private StaticConfiguration() {
    }

    private static Validator buildValidator() {
        final JsonSchema schema = JsonSchema.of(buildSchema().toJson());
        final JsonSchemaOptions options = new JsonSchemaOptions().setDraft(Draft.DRAFT202012)
            .setBaseUri("https://inventage.com/portal-gateway/static-configuration");
        return Validator.create(schema, options);
    }

    public static ObjectSchemaBuilder buildSchema() {
        final ObjectSchemaBuilder entrypointSchema = Schemas.objectSchema()
            .requiredProperty(ENTRYPOINT_NAME, Schemas.stringSchema().withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .requiredProperty(ENTRYPOINT_PORT, Schemas.anyOf(Schemas.schema()
                .withKeyword(KEYWORD_TYPE, INT_TYPE),
                Schemas.schema()
                    .withKeyword(KEYWORD_TYPE, STRING_TYPE)
                    .withKeyword(KEYWORD_PATTERN, ENV_VARIABLE_PATTERN_STRING_TO_INT)))
            .property(DynamicConfiguration.MIDDLEWARES,
                Schemas.arraySchema().items(DynamicConfiguration.getBuildMiddlewareSchema()))
            .property(ENTRYPOINT_SESSION_IDLE_TIMEOUT, Schemas.intSchema().withKeyword(KEYWORD_INT_MIN, NON_ZERO_INT_MIN))
            .allowAdditionalProperties(false);

        final ObjectSchemaBuilder applicationSchema = Schemas.objectSchema()
            .requiredProperty(APPLICATION_NAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .requiredProperty(APPLICATION_ENTRYPOINT, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .requiredProperty(APPLICATION_REQUEST_SELECTOR,
                Schemas.objectSchema().requiredProperty(APPLICATION_REQUEST_SELECTOR_URL_PREFIX,
                    Schemas.stringSchema().withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH)))
            .requiredProperty(APPLICATION_PROVIDER, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .allowAdditionalProperties(false);

        final ObjectSchemaBuilder providerSchema = Schemas.objectSchema()
            .requiredProperty(PROVIDER_NAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(PROVIDER_FILE_FILENAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(PROVIDER_FILE_DIRECTORY, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(PROVIDER_FILE_WATCH, Schemas.booleanSchema())
            .property(PROVIDER_DOCKER_ENDPOINT, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(PROVIDER_DOCKER_EXPOSED_BY_DEFAULT, Schemas.booleanSchema())
            .property(PROVIDER_DOCKER_NETWORK, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(PROVIDER_DOCKER_DEFAULT_RULE, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .allowAdditionalProperties(false);

        final ObjectSchemaBuilder staticConfigBuilder = Schemas.objectSchema()
            .property(ENTRYPOINTS, Schemas.arraySchema().items(entrypointSchema))
            .property(APPLICATIONS, Schemas.arraySchema().items(applicationSchema))
            .property(PROVIDERS, Schemas.arraySchema().items(providerSchema));

        return staticConfigBuilder;
    }

    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        if (validator == null) {
            validator = buildValidator();
        }

        final Promise<Void> validPromise = Promise.promise();
        try {
            final OutputUnit result = validator.validate(json);
            if (!result.getValid()) {
                throw result.toException(json);
            }
        } catch (SchemaException | ValidationException e) {
            validPromise.fail(e);
            return validPromise.future();
        }

        final List<Future<Void>> futures = validateEntrypoints(json.getJsonArray(ENTRYPOINTS));
        futures.add(validateProviders(json.getJsonArray(PROVIDERS)));
        Future.all(futures).onSuccess(cf -> {
            validPromise.complete();
        }).onFailure(cfErr -> {
            validPromise.fail(cfErr.getMessage());
        });

        return validPromise.future();
    }

    private static List<Future<Void>> validateEntrypoints(JsonArray entrypoints) {
        final List<Future<Void>> middlewareFutures = new ArrayList<>();
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
