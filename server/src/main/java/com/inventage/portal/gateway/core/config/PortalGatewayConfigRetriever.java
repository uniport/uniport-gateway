package com.inventage.portal.gateway.core.config;

import java.io.File;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Json file for the portal gateway configuration is taken from one of these places: 1. File
 * pointed to by the system env variable 'PORTAL_GATEWAY_JSON' 2. File pointed to by the system
 * property 'PORTAL_GATEWAY_JSON' 3. File 'portal-gateway.json' in '/etc/portal-gateway/' 4. File
 * 'portal-gateway.json' in the current working directory
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

    /**
     * Stores added later to the options, will override properties from prior stores.
     *
     * @return ConfigRetrieverOptions
     */
    private static ConfigRetrieverOptions getOrCreateOptions() {
        if (options == null) {
            options = new ConfigRetrieverOptions();

            getPortalGatewayJson().ifPresent(json -> options.addStore(json));
            options.addStore(new ConfigStoreOptions().setType("sys")
                    .setConfig(new JsonObject().put("raw-data", true)));
            options.addStore(new ConfigStoreOptions().setType("env")
                    .setConfig(new JsonObject().put("raw-data", true)));
        }
        return options;
    }

    private static Optional<ConfigStoreOptions> getPortalGatewayJson() {
        final String envValue = System.getenv(PROPERTY);
        if (existsAsFile(envValue)) {
            LOGGER.info("getPortalGatewayJson: reading from system env variable as '{}'", envValue);
            return Optional.of(configStoreOptions(envValue));
        }
        final String sysValue = System.getProperty(PROPERTY);
        if (existsAsFile(sysValue)) {
            LOGGER.info("getPortalGatewayJson: reading from system property as '{}'", sysValue);
            return Optional.of(configStoreOptions(envValue));
        }
        if (existsAsFile(DEFAULT_CONFIG_FILE_PATH)) {
            LOGGER.info("getPortalGatewayJson: reading from default file '{}'",
                    DEFAULT_CONFIG_FILE_PATH);
            return Optional.of(configStoreOptions(DEFAULT_CONFIG_FILE_PATH));
        }
        if (existsAsFile(LOCAL_CONFIG_FILE_PATH)) {
            LOGGER.info("getPortalGatewayJson: reading file '{}' from working directory '{}'",
                    LOCAL_CONFIG_FILE_PATH, new File(".").getAbsolutePath());
            return Optional.of(configStoreOptions(LOCAL_CONFIG_FILE_PATH));
        }
        LOGGER.warn("getPortalGatewayJson: no portal-gateway.json file configured");
        return Optional.empty();
    }

    private static ConfigStoreOptions configStoreOptions(String filePath) {
        String fileName = String.format("%s/%s", filePath, DEFAULT_CONFIG_FILE_NAME);
        final File file = new File(fileName);
        return new ConfigStoreOptions().setType("file")
                .setFormat(file.getName().endsWith("json") ? "json" : "properties").setConfig(
                        new JsonObject().put("path", file.getAbsolutePath()).put("raw-data", true));
    }

    private static boolean existsAsFile(String filePath) {
        String fileName = String.format("%s/%s", filePath, DEFAULT_CONFIG_FILE_NAME);
        return fileName != null && new File(fileName).exists();
    }

}
