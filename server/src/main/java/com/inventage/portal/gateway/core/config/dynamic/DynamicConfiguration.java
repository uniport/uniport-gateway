package com.inventage.portal.gateway.core.config.dynamic;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;

public class DynamicConfiguration {
    // keywords used for internal purpose only
    public static final String HTTP = "http";
    public static final String ROUTERS = "routers";
    public static final String ROUTER_NAME = "name";
    public static final String ROUTER_ENTRYPOINTS = "entrypoints";
    public static final String ROUTER_SERVICE = "service";
    public static final String ROUTER_RULE = "rule";
    public static final String MIDDLEWARES = "middlewares";
    public static final String MIDDLEWARE_NAME = "name";
    public static final String MIDDLEWARE_TYPE = "type";
    public static final String SERVICES = "services";
    public static final String SERVICE_NAME = "name";
    public static final String SERVICE_SERVERS = "server";
    public static final String SERVICE_SERVER_SCHEMA = "schema";
    public static final String SERVICE_SERVER_URL = "url";
    public static final String SERVICE_SERVER_PORT = "port";

    private static Schema schema;

    private static Schema buildSchema(Vertx vertx) {
        ObjectSchemaBuilder routerSchema = Schemas.objectSchema().requiredProperty(ROUTER_NAME, Schemas.stringSchema())
                .requiredProperty(ROUTER_ENTRYPOINTS, Schemas.arraySchema().items(Schemas.stringSchema()))
                .property(MIDDLEWARES, Schemas.arraySchema().items(Schemas.stringSchema()))
                .requiredProperty(ROUTER_SERVICE, Schemas.stringSchema()).property(ROUTER_RULE, Schemas.stringSchema());

        ObjectSchemaBuilder middlewareSchema = Schemas.objectSchema()
                .requiredProperty(MIDDLEWARE_NAME, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_TYPE, Schemas.objectSchema());

        ObjectSchemaBuilder serviceSchema = Schemas.objectSchema()
                .requiredProperty(SERVICE_NAME, Schemas.stringSchema())
                .requiredProperty(SERVICE_SERVERS, Schemas.arraySchema()
                        .items(Schemas.objectSchema().requiredProperty(SERVICE_SERVER_SCHEMA, Schemas.stringSchema())
                                .requiredProperty(SERVICE_SERVER_URL, Schemas.stringSchema())
                                .requiredProperty(SERVICE_SERVER_PORT, Schemas.intSchema())));

        ObjectSchemaBuilder httpSchema = Schemas.objectSchema()
                .property(ROUTERS, Schemas.arraySchema().items(routerSchema))
                .property(MIDDLEWARES, Schemas.arraySchema().items(middlewareSchema))
                .property(SERVICES, Schemas.arraySchema().items(serviceSchema));

        ObjectSchemaBuilder dynamicConfigBuilder = Schemas.objectSchema().property(HTTP, httpSchema);

        SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createDraft201909SchemaParser(schemaRouter);
        return dynamicConfigBuilder.build(schemaParser);
    }

    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        if (schema == null) {
            schema = buildSchema(vertx);
        }
        return schema.validateAsync(json);
    }
}