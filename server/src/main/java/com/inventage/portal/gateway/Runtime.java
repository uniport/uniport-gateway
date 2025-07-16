package com.inventage.portal.gateway;

import io.vertx.core.Vertx;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runtime {

    public static final String DEVELOPMENT_MODE_KEY = "DEVELOPMENT";
    public static final String VERTICLE_INSTANCES_KEY = "VERTICLE_INSTANCES";

    public static final String CLUSTERED_KEY = "PORTAL_GATEWAY_CLUSTERED";

    private static final String INSTANCE_NAME_PROPERTY = "PORTAL_GATEWAY_INSTANCE_NAME";
    public static final String DEFAULT_INSTANCE_NAME = "unknown";

    public static final String LOGGING_CONFIG_PROPERTY = "PORTAL_GATEWAY_LOGGING_CONFIG";
    public static final String STRUCTURAL_LOGGING_ENABLED_PROPERTY = "PORTAL_GATEWAY_STRUCTURAL_LOGGING_ENABLED";

    private static final String DEFAULT_LOGGING_CONFIG_FILE_PATH = "/etc/uniport-gateway";
    private static final String DEFAULT_STRUCTURED_LOGGING_CONFIG_FILE_NAME = "logback.xml";
    private static final String DEFAULT_UNSTRUCTURED_LOGGING_CONFIG_FILE_NAME = "logback-unstructured.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    private Runtime() {
    }

    /**
     * Development mode can be activated by setting the environment variable 'DEVELOPMENT'.
     *
     * @return true if development mode is activated
     */
    public static boolean isDevelopment() {
        final boolean envValue = toBoolean(System.getenv(DEVELOPMENT_MODE_KEY));
        final boolean propValue = toBoolean(System.getProperty(DEVELOPMENT_MODE_KEY));
        if (envValue || propValue) {
            LOGGER.warn("Development mode is active");
            return true;
        }
        return false;
    }

    /**
     * returns the number of verticles.
     *
     * @return number
     */
    public static int numberOfVerticleInstances() {
        try {
            final int envValue = toInt(System.getenv(VERTICLE_INSTANCES_KEY));
            LOGGER.info("Number of verticles from environment is '{}'", envValue);
            return envValue;
        } catch (Exception e) {
            // Noop…
        }
        try {
            final int propvalue = toInt(System.getProperty(VERTICLE_INSTANCES_KEY));
            LOGGER.info("Number of verticles from system property is '{}'", propvalue);
            return propvalue;
        } catch (Exception e) {
            // Noop…
        }
        final int defaultNumber = 1;
        LOGGER.info("Number of verticles from default is '{}'", defaultNumber);
        return defaultNumber;
    }

    /**
     * Cluster mode can be activated by setting the system property 'clustered'
     * (-Dclustered).
     *
     * @return true if cluster mode is activated
     */
    public static boolean isClustered() {
        final boolean envValue = toBoolean(System.getenv(CLUSTERED_KEY));
        final boolean propValue = toBoolean(System.getProperty(CLUSTERED_KEY));
        if (envValue || propValue) {
            LOGGER.info("Running in cluster mode");
            return true;
        }
        return false;
    }

    public static String getInstanceName() {
        return System.getenv().getOrDefault(INSTANCE_NAME_PROPERTY, DEFAULT_INSTANCE_NAME);
    }

    /**
     * This logs the error message and shuts the irrecoverably Gateway down.
     * 
     * @param vertx
     */
    public static void fatal(Vertx vertx, String errMsg) {
        vertx.close().onComplete(event -> {
            LOGGER.error("Gateway is shutting down: '{}'", errMsg);
        });
    }

    /**
     * The logback.xml for the logback configuration is taken from one of these places:
     * 1. File pointed to by the env variable 'PORTAL_GATEWAY_LOGGING_CONFIG'
     * 2. File pointed to by the system property 'PORTAL_GATEWAY_LOGGING_CONFIG'
     * 3. File 'logback.xml' in '/etc/uniport-gateway', if PORTAL_GATEWAY_STRUCTURAL_LOGGING_ENABLED=true
     * 4. File 'logback-unstructured.xml' in '/etc/uniport-gateway', if PORTAL_GATEWAY_STRUCTURAL_LOGGING_ENABLED=false
     * 5. Normal configuration discovery process as configured by logback
     */
    public static Optional<Path> getLoggingConfigPath() {
        // take path from env var
        String loggingConfigFilePath = System.getenv(LOGGING_CONFIG_PROPERTY);
        if (existsAsFile(loggingConfigFilePath)) {
            return Optional.of(Path.of(loggingConfigFilePath));
        } else {
            if (loggingConfigFilePath != null) {
                System.err.println("File '" + new File(loggingConfigFilePath).getAbsoluteFile() + "' for system env '" + LOGGING_CONFIG_PROPERTY + "' not found");
            }
        }

        loggingConfigFilePath = System.getProperty(LOGGING_CONFIG_PROPERTY);
        if (existsAsFile(loggingConfigFilePath)) {
            return Optional.of(Path.of(loggingConfigFilePath));
        } else {
            if (loggingConfigFilePath != null) {
                System.err.println("File '" + new File(loggingConfigFilePath).getAbsoluteFile() + "' for system property '" + LOGGING_CONFIG_PROPERTY + "' not found");
            }
        }

        final String loggingConfigFileName;
        if (isStructuredLoggingEnabled()) {
            loggingConfigFileName = DEFAULT_STRUCTURED_LOGGING_CONFIG_FILE_NAME;
        } else {
            loggingConfigFileName = DEFAULT_UNSTRUCTURED_LOGGING_CONFIG_FILE_NAME;
        }

        // take path from the default path
        loggingConfigFilePath = String.format("%s/%s", DEFAULT_LOGGING_CONFIG_FILE_PATH, loggingConfigFileName);
        if (existsAsFile(loggingConfigFilePath)) {
            return Optional.of(Path.of(loggingConfigFilePath));
        } else {
            System.err.println("File '" + new File(loggingConfigFilePath).getAbsoluteFile() + "' not found");
        }

        return Optional.empty();
    }

    private static boolean isStructuredLoggingEnabled() {
        final boolean envValue = toBoolean(System.getenv(STRUCTURAL_LOGGING_ENABLED_PROPERTY));
        final boolean propValue = toBoolean(System.getProperty(STRUCTURAL_LOGGING_ENABLED_PROPERTY));
        if (envValue || propValue) {
            LOGGER.info("Structured logging is enabled.");
            return true;
        }
        return false;
    }

    private static int toInt(String property) throws NumberFormatException {
        return Integer.parseInt(property);
    }

    private static boolean toBoolean(String property) {
        return Boolean.parseBoolean(property);
    }

    private static boolean existsAsFile(String fileName) {
        return fileName != null && new File(fileName).exists();
    }
}
