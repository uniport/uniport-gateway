package com.inventage.portal.gateway;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class JsonSchemaGenerator {

    private JsonSchemaGenerator() {
    }

    public static void main(String[] args) throws IOException {
        String path = "";
        if (args.length >= 1 && args[0] != null) {
            path = args[0];
        }
        createStaticJsonSchemaValidator(path);
        createDynamicJsonSchemaValidator(path);
    }

    private static void createStaticJsonSchemaValidator(String path) throws IOException {
        final ObjectSchemaBuilder schema = StaticConfiguration.buildSchema();
        final JsonObject schemaAsJson = schema.toJson();
        removeId(schemaAsJson);
        Files.write(Paths.get(path + "portalGatewayStaticSchema.json"), schemaAsJson.encodePrettily().getBytes(StandardCharsets.UTF_8));

    }

    private static void createDynamicJsonSchemaValidator(String path) throws IOException {
        final ObjectSchemaBuilder schema = DynamicConfiguration.buildSchema();
        final JsonObject schemaAsJson = schema.toJson();
        removeId(schemaAsJson);
        Files.write(Paths.get(path + "portalGatewayDynamicSchema.json"), schemaAsJson.encodePrettily().getBytes(StandardCharsets.UTF_8));

    }

    private static void removeId(JsonObject jsonObject) {
        jsonObject.remove("$id");
        for (String key : jsonObject.fieldNames()) {
            final Object value = jsonObject.getValue(key);
            if (value instanceof JsonObject) {
                removeId((JsonObject) value);
            } else if (value instanceof JsonArray) {
                final JsonArray jsonArray = (JsonArray) value;
                for (int i = 0; i < jsonArray.size(); i++) {
                    final Object arrayValue = jsonArray.getValue(i);
                    if (arrayValue instanceof JsonObject) {
                        removeId((JsonObject) arrayValue);
                    }
                }
            }
        }
    }
}
