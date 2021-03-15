package com.inventage.portal.gateway.core.provider;

import java.util.Optional;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface ProviderFactory {

    Logger LOGGER = LoggerFactory.getLogger(ProviderFactory.class);

    String provides();

    AbstractProvider create(Vertx vertx, String configurationAddress, JsonObject providerConfig);

    class Loader {
        public static ProviderFactory getFactory(String providerName) {
            LOGGER.debug("getFactory: for '{}'", providerName);
            final Optional<ProviderFactory> provider = ServiceLoader.load(ProviderFactory.class)
                    .stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(providerName)).findFirst();
            if (provider.isPresent()) {
                return provider.get();
            } else {
                throw new IllegalStateException(
                        String.format("Provider factory '%s' doesn't exist!", providerName));
            }
        }
    }
}
