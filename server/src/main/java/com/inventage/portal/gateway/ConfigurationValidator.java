package com.inventage.portal.gateway;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import java.io.FileWriter;
import java.io.IOException;

public final class ConfigurationValidator {

    private ConfigurationValidator() {
    }

    public static void main(String[] args) {
        String path = "";
        if (args.length >= 1 && args[0] != null) {
            path = args[0];
        }
        createStaticJsonSchemaValidator(path);
        createDynamicJsonSchemaValidator(path);
    }

    private static void createStaticJsonSchemaValidator(String path) {
        final JsonSchema schema = StaticConfiguration.buildSchema();
        final JsonObject schemaAsJson = schema.resolve();
        try (FileWriter file = new FileWriter(path + "static.json")) {
            file.write(schemaAsJson.encodePrettily());
            file.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createDynamicJsonSchemaValidator(String path) {
        final JsonSchema schema = DynamicConfiguration.buildSchema();
        final JsonObject schemaAsJson = schema.resolve();
        try (FileWriter file = new FileWriter(path + "dynamic.json")) {
            file.write(schemaAsJson.encodePrettily());
            file.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
