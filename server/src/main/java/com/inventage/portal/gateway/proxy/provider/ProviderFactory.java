package com.inventage.portal.gateway.proxy.provider;

import java.util.Optional;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Service interface for providing providers. Implementations must add an entry with the fully
 * qualified class name into
 * META-INF/services/com.inventage.portal.gateway.proxy.provider.ProviderFactory
 */
public interface ProviderFactory {

    final Logger LOGGER = LoggerFactory.getLogger(ProviderFactory.class);

    String provides();

    Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig, JsonObject env);

    class Loader {
        public static ProviderFactory getFactory(String providerName) {
            LOGGER.debug("Get provider factory '{}'", providerName);
            final Optional<ProviderFactory> provider = ServiceLoader.load(ProviderFactory.class).stream()
                    .map(ServiceLoader.Provider::get).filter(instance -> instance.provides().equals(providerName))
                    .findFirst();
            if (provider.isPresent()) {
                return provider.get();
            }
            return null;
        }
    }
}
