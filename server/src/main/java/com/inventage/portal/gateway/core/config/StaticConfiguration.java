package com.inventage.portal.gateway.core.config;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputFormat;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.Validator;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticConfiguration.class);

    private static Validator validator;

    // schema
    private static final Pattern ENV_VARIABLE_PATTERN = Pattern.compile("^\\$\\{.*\\}$");

    private StaticConfiguration() {
    }

    private static Validator buildValidator() {
        final JsonSchema schema = JsonSchema.of(buildSchema().toJson());
        final JsonSchemaOptions options = new JsonSchemaOptions()
            .setDraft(Draft.DRAFT202012)
            .setOutputFormat(OutputFormat.Basic)
            .setBaseUri("https://inventage.com/portal-gateway/static-configuration");
        return Validator.create(schema, options);
    }

    public static ObjectSchemaBuilder buildSchema() {
        final ObjectSchemaBuilder entrypointSchema = Schemas.objectSchema()
            .requiredProperty(ENTRYPOINT_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(ENTRYPOINT_PORT, Schemas.anyOf(Schemas.schema()
                .with(Keywords.type(SchemaType.INTEGER)),
                Schemas.schema()
                    .with(Keywords.type(SchemaType.STRING))
                    .with(Keywords.pattern(ENV_VARIABLE_PATTERN))))
            .optionalProperty(DynamicConfiguration.MIDDLEWARES,
                Schemas.arraySchema()
                    .items(Schemas.oneOf(DynamicConfiguration.getBuildMiddlewareSchema())))
            .optionalProperty(ENTRYPOINT_SESSION_IDLE_TIMEOUT, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(1)))
            .allowAdditionalProperties(false);

        final ObjectSchemaBuilder applicationSchema = Schemas.objectSchema()
            .requiredProperty(APPLICATION_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(APPLICATION_ENTRYPOINT, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .requiredProperty(APPLICATION_REQUEST_SELECTOR,
                Schemas.objectSchema().requiredProperty(APPLICATION_REQUEST_SELECTOR_URL_PREFIX,
                    Schemas.stringSchema().with(Keywords.minLength(1))))
            .requiredProperty(APPLICATION_PROVIDER, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);

        final ObjectSchemaBuilder providerSchema = Schemas.objectSchema()
            .requiredProperty(PROVIDER_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(PROVIDER_FILE_FILENAME, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(PROVIDER_FILE_DIRECTORY, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(PROVIDER_FILE_WATCH, Schemas.booleanSchema())
            .optionalProperty(PROVIDER_DOCKER_ENDPOINT, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(PROVIDER_DOCKER_EXPOSED_BY_DEFAULT, Schemas.booleanSchema())
            .optionalProperty(PROVIDER_DOCKER_NETWORK, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(PROVIDER_DOCKER_DEFAULT_RULE, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);

        final ObjectSchemaBuilder staticConfigBuilder = Schemas.objectSchema()
            .optionalProperty(ENTRYPOINTS, Schemas.arraySchema().items(entrypointSchema))
            .optionalProperty(APPLICATIONS, Schemas.arraySchema().items(applicationSchema))
            .optionalProperty(PROVIDERS, Schemas.arraySchema().items(providerSchema));

        return staticConfigBuilder;
    }

    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        if (validator == null) {
            validator = buildValidator();
        }

        final OutputUnit result;
        try {
            result = validator.validate(json);
        } catch (SchemaException e) {
            return Future.failedFuture(e);
        }
        if (result.getValid() == null || !result.getValid()) {
            // the error message for items/oneOf validation failure is unusable, so we try being more helpful
            if (result.getErrors() != null) {
                for (final OutputUnit error : result.getErrors()) {
                    final String keyword = error.getKeywordLocation();
                    final String instance = error.getInstanceLocation();
                    final String message = error.getError();

                    if (keyword.endsWith("items/oneOf")) {
                        LOGGER.warn("{} at '{}':\n{}", message, instance, DynamicConfiguration.resolveOneOfError(json, instance));
                    }
                }
            }

            return Future.failedFuture(result.toJson().encodePrettily());
        }

        final List<Future<Void>> futures = validateEntrypoints(json.getJsonArray(ENTRYPOINTS));
        futures.add(validateProviders(json.getJsonArray(PROVIDERS)));
        return Future.all(futures).mapEmpty();
    }

    private static List<Future<Void>> validateEntrypoints(JsonArray entrypoints) {
        final List<Future<Void>> middlewareFutures = new ArrayList<>();
        if (entrypoints != null) {
            for (int i = 0; i < entrypoints.size(); i++) {
                final JsonObject entrypoint = entrypoints.getJsonObject(i);
                final JsonArray middlewares = entrypoint.getJsonArray(DynamicConfiguration.MIDDLEWARES);
                middlewareFutures.add(validateEntryMiddlewares(middlewares));
            }
        }
        return middlewareFutures;
    }

    private static Future<Void> validateEntryMiddlewares(JsonArray entryMiddlewares) {
        return DynamicConfiguration.validateMiddlewares(entryMiddlewares, false).mapEmpty();
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
