package com.inventage.portal.gateway.core.application;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Service interface for providing applications.
 * Implementations must add an entry with the fully qualified class name into
 * META-INF/services/com.inventage.portal.gateway.core.application.ApplicationProvider
 */
public interface ApplicationProvider {

    Logger LOGGER = LoggerFactory.getLogger(ApplicationProvider.class);

    /**
     * Used in the portal-gateway.json applications.provider field.
     * @return normally the fully qualified class name
     */
    String provides();

    /**
     *
     * @param applicationConfig extract of the config for this application
     * @param config complete config
     * @param vertx running instance by which an application can get their router
     * @return new application instance
     */
    Application create(JsonObject applicationConfig, JsonObject config, Vertx vertx);

    class Loader {
        public static ApplicationProvider getProvider(String providerId) {
            LOGGER.debug("getProvider: for '{}'", providerId);
            final Optional<ApplicationProvider> provider = ServiceLoader.load(ApplicationProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(providerId))
                    .findFirst();
            if (provider.isPresent()) {
                return provider.get();
            }
            else {
                throw new IllegalStateException(String.format("Application provider '%s' doesn't exist!", providerId));
            }
        }
    }

}
