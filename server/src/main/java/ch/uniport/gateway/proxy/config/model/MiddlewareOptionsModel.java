package ch.uniport.gateway.proxy.config.model;

import org.slf4j.Logger;

public interface MiddlewareOptionsModel {

    /**
     * A middleware option POJO commonly needs to validate a its configuration and
     * is responsible for setting default values.
     * This method can be used to log the absence of optional configuration values
     * and what default values are set instead.
     * 
     * @param logger
     * @param key
     * @param defaultValue
     */
    default void logDefault(Logger logger, String key, Object defaultValue) {
        logger.debug("'{}' not configured. Using default value: '{}'", key, defaultValue);
    }

    default void logDefault(Logger logger, String key) {
        logger.debug("'{}' not configured. Using default value", key);
    }

}
