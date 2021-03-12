package com.inventage.portal.gateway.core.provider;

import java.util.Optional;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;

public interface ProviderFactory {

    Logger LOGGER = LoggerFactory.getLogger(Provider.class);

    String provides();

    AbstractProvider create(String configurationAddress, JsonObject providerConfig);

    class Loader {
        public static ProviderFactory getFactory(String providerName) {
            LOGGER.debug("getFactory: for '{}'", providerName);
            final Optional<ProviderFactory> provider = ServiceLoader.load(ProviderFactory.class).stream()
                    .map(ServiceLoader.Provider::get).filter(instance -> instance.provides().equals(providerName))
                    .findFirst();
            if (provider.isPresent()) {
                return provider.get();
            } else {
                throw new IllegalStateException(
                        String.format("Application provider '%s' doesn't exist!", providerName));
            }
        }
    }
}
