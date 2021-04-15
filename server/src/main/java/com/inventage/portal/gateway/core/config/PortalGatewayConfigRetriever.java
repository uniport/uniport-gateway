package com.inventage.portal.gateway.core.config;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Json file for the portal gateway configuration is taken from one of these places: 
 * 1. File pointed to by the system env variable 'PORTAL_GATEWAY_JSON' 
 * 2. File pointed to by the system property 'PORTAL_GATEWAY_JSON' 
 * 3. File 'portal-gateway.json' in '/etc/portal-gateway/' 
 * 4. File 'portal-gateway.json' in the current working directory
 */
public class PortalGatewayConfigRetriever {

    public static final String DEFAULT_CONFIG_FILE_NAME = "portal-gateway.json";
    public static final String PROPERTY = "PORTAL_GATEWAY_JSON";
    public static final String DEFAULT_CONFIG_FILE_PATH = "/etc/portal-gateway";
    public static final String LOCAL_CONFIG_FILE_PATH = ".";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PortalGatewayConfigRetriever.class);
    private static ConfigRetrieverOptions options;

    /**
     * returns a ConfigRetriever.
     *
     * @param vertx vertx instance
     * @return ConfigRetriever
     */
    public static ConfigRetriever create(Vertx vertx) {
        return ConfigRetriever.create(vertx, getOrCreateOptions());
    }

    public static Optional<Path> getStaticConfigPath() {
        // 1.
        String staticConfigFileName = System.getenv(PROPERTY);
        if (existsAsFile(staticConfigFileName)) {
            LOGGER.info("getPortalGatewayJson: reading from system env variable as '{}'",
                    staticConfigFileName);
            return Optional.of(Path.of(staticConfigFileName));
        }
        // 2.
        staticConfigFileName = System.getProperty(PROPERTY);
        if (existsAsFile(staticConfigFileName)) {
            LOGGER.info("getPortalGatewayJson: reading from system property as '{}'",
                    staticConfigFileName);
            return Optional.of(Path.of(staticConfigFileName));
        }
        // 3.
        staticConfigFileName =
                String.format("%s/%s", DEFAULT_CONFIG_FILE_PATH, DEFAULT_CONFIG_FILE_NAME);
        if (existsAsFile(staticConfigFileName)) {
            LOGGER.info("getPortalGatewayJson: reading from default file '{}'",
                    staticConfigFileName);
            return Optional.of(Path.of(staticConfigFileName));
        }
        // 4.
        staticConfigFileName =
                String.format("%s/%s", LOCAL_CONFIG_FILE_PATH, DEFAULT_CONFIG_FILE_NAME);
        if (existsAsFile(staticConfigFileName)) {
            LOGGER.info(
                    "getPortalGatewayJson: reading from default file within working directory '{}'",
                    staticConfigFileName);
            return Optional.of(Path.of(staticConfigFileName));
        }
        LOGGER.warn("getPortalGatewayJson: no portal-gateway.json file configured");
        return Optional.empty();
    }

    /**
     * Stores added later to the options, will override properties from prior stores.
     *
     * @return ConfigRetrieverOptions
     */
    private static ConfigRetrieverOptions getOrCreateOptions() {
        if (options == null) {
            options = new ConfigRetrieverOptions();

            getPortalGatewayJson().ifPresent(json -> options.addStore(json));
            options.addStore(new ConfigStoreOptions().setType("env")
                    .setConfig(new JsonObject().put("raw-data", true)));
        }
        return options;
    }

    /**
     * Implements the strategy for finding the static configuration file.
     *
     * @return
     */
    private static Optional<ConfigStoreOptions> getPortalGatewayJson() {
        Optional<Path> staticConfigPath = getStaticConfigPath();
        if (staticConfigPath.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(configStoreOptions(staticConfigPath.get()));
    }

    private static ConfigStoreOptions configStoreOptions(Path filePath) {
        return new ConfigStoreOptions().setType("file")
                .setFormat(filePath.toString().endsWith("json") ? "json" : "properties")
                .setConfig(new JsonObject().put("path", filePath.toAbsolutePath()).put("raw-data",
                        true));
    }

    private static boolean existsAsFile(String fileName) {
        return fileName != null && new File(fileName).exists();
    }

}
