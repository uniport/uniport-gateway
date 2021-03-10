package com.inventage.portal.gateway.core.config.startup;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;

public class StaticConfiguration {

    public static final String ENTRYPOINTS = "entrypoints";
    public static final String ENTRYPOINT_NAME = "name";
    public static final String ENTRYPOINT_PORT = "port";
    public static final String APPLICATIONS = "applications";
    public static final String APPLICATION_NAME = "name";
    public static final String APPLICATION_ENTRYPOINT = "entrypoint";
    public static final String APPLICATION_REQUEST_SELECTOR = "requestSelector";
    public static final String APPLICATION_REQUEST_SELECTOR_URL_PREFIX = "urlPrefix";
    public static final String APPLICATION_PROVIDER = "provider";
    public static final String PROVIDERS = "providers";
    public static final String PROVIDER_NAME = "name";

    private static Schema schema;

    private static Schema buildSchema(Vertx vertx) {

        ObjectSchemaBuilder entrypointSchema = Schemas.objectSchema()
                .requiredProperty(ENTRYPOINT_NAME, Schemas.stringSchema())
                .requiredProperty(ENTRYPOINT_PORT, Schemas.intSchema());

        ObjectSchemaBuilder applicationSchema = Schemas.objectSchema()
                .requiredProperty(APPLICATION_NAME, Schemas.stringSchema())
                .requiredProperty(APPLICATION_ENTRYPOINT, Schemas.stringSchema())
                .requiredProperty(APPLICATION_REQUEST_SELECTOR, Schemas.objectSchema()
                        .requiredProperty(APPLICATION_REQUEST_SELECTOR_URL_PREFIX, Schemas.stringSchema()))
                .requiredProperty(APPLICATION_PROVIDER, Schemas.stringSchema());

        ObjectSchemaBuilder providerSchema = Schemas.objectSchema().requiredProperty(PROVIDER_NAME,
                Schemas.stringSchema());

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
        return schema.validateAsync(json);
    }
}
