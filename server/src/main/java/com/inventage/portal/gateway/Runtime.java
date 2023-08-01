package com.inventage.portal.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runtime {

    public static final String DEVELOPMENT_MODE_KEY = "development";
    public static final String VERTICLE_INSTANCES_KEY = "verticle.instances";
    public static final String CLUSTERED_KEY = "PORTAL_GATEWAY_CLUSTERED";

    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    private Runtime() {
    }

    /**
     * Development mode can be activated by setting the system property 'development'
     * (-Ddevelopment).
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
            LOGGER.warn("Number of verticles from system property is '{}'", propvalue);
            return propvalue;
        } catch (Exception e) {
            // Noop…
        }
        final int defaultNumber = 1;
        LOGGER.warn("Number of verticles from default is '{}'", defaultNumber);
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
            LOGGER.warn("Running in cluster mode");
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

}
