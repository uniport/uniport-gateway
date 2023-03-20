package com.inventage.portal.gateway.core.application;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service interface for providing applications. Implementations must add an entry with the fully
 * qualified class name into
 * META-INF/services/com.inventage.portal.gateway.core.application.ApplicationFactory
 */
public interface ApplicationFactory {

    Logger LOGGER = LoggerFactory.getLogger(ApplicationFactory.class);

    /**
     * Used in the portal-gateway.json applications.provider field.
     *
     * @return normally the fully qualified class name
     */
    String provides();

    /**
     * @param applicationConfig
     *            extract of the config for this application
     * @param globalConfig
     *            complete config
     * @param vertx
     *            running instance by which an application can get their router
     * @return new application instance
     */
    Application create(JsonObject applicationConfig, JsonObject globalConfig, Vertx vertx);

    class Loader {
        public static ApplicationFactory getProvider(String providerId) {
            LOGGER.debug("Get application provider for '{}'", providerId);
            final Optional<ApplicationFactory> provider = ServiceLoader.load(ApplicationFactory.class).stream()
                .map(ServiceLoader.Provider::get).filter(instance -> instance.provides().equals(providerId))
                .findFirst();
            if (provider.isPresent()) {
                return provider.get();
            } else {
                throw new IllegalStateException(String.format("Application provider '%s' doesn't exist!", providerId));
            }
        }
    }

}
